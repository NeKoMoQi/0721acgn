"""Read existing animation inventory; never insert new works or change the database."""
import argparse
from collections import defaultdict
import json
import os
from pathlib import Path
import re
import sys
from urllib.parse import urlparse


def from_dump(path):
    text = path.read_text(encoding="utf-8-sig")
    works, episodes = [], []
    # Extract only numeric row prefixes and the name. Do not execute this dump:
    # some bundled text fields contain historical encoding damage.
    for line in text.splitlines():
        if line.startswith("INSERT INTO `tb_animation` VALUES "):
            works.extend((int(m[2]), m[3]) for m in re.finditer(r"\((\d+),(\d+),'([^']*)'", line))
        elif line.startswith("INSERT INTO `tb_animation_episode` VALUES "):
            episodes.extend((int(m[2]), int(m[3])) for m in re.finditer(r"\((\d+),(\d+),(\d+),'", line))
    if not works:
        raise ValueError("No supported tb_animation row prefixes found")
    return works, episodes


def database_config(properties):
    values = {}
    if properties.is_file():
        for line in properties.read_text(encoding="utf-8-sig").splitlines():
            if "=" in line and not line.lstrip().startswith(("#", "!")):
                key, value = line.split("=", 1)
                values[key.strip()] = value.strip()

    def get(env, key, fallback=""):
        value = os.environ.get(env, values.get(key, fallback))
        match = re.fullmatch(r"\$\{([^}:]+)(?::([^}]*))?\}", value)
        if match:
            value = os.environ.get(match[1], match[2])
            if value is None:
                raise ValueError("A configuration environment variable is missing")
        if "${" in value or value.startswith("YOUR_") or "\\" in value:
            raise ValueError("Unsupported/placeholder property; use MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD environment variables")
        return value

    jdbc = get("MYSQL_URL", "spring.datasource.mysql.jdbc-url", values.get("spring.datasource.mysql.url", ""))
    parsed = urlparse(jdbc.removeprefix("jdbc:"))
    username = get("MYSQL_USER", "spring.datasource.mysql.username")
    if parsed.scheme != "mysql" or not parsed.hostname or not parsed.path.strip("/") or not username:
        raise ValueError("No usable MySQL configuration; pass --properties with the real backend configuration")
    return {"host": parsed.hostname, "port": parsed.port or 3306, "database": parsed.path.strip("/"),
            "user": username, "password": get("MYSQL_PASSWORD", "spring.datasource.mysql.password"),
            "charset": "utf8mb4", "connect_timeout": 15, "read_timeout": 30, "write_timeout": 30}


def from_database(properties):
    import pymysql
    with pymysql.connect(**database_config(properties)) as connection:
        with connection.cursor() as cursor:
            cursor.execute("SET TRANSACTION READ ONLY")
            connection.begin()
            try:
                cursor.execute("SELECT id,name FROM tb_animation ORDER BY id")
                works = cursor.fetchall()
                cursor.execute("SELECT animation_id,episode_no FROM tb_animation_episode ORDER BY animation_id,episode_no")
                return works, cursor.fetchall()
            finally:
                connection.rollback()


def inventory(works, episodes):
    by_id = defaultdict(set)
    for animation_id, number in episodes:
        by_id[int(animation_id)].add(int(number))
    rows = []
    for animation_id, name in works:
        present = sorted(by_id[int(animation_id)])
        rows.append({"animation_id": int(animation_id), "name": name,
                     "recorded_episode_count": len(present), "recorded_episodes": present,
                     "gaps_below_recorded_max": sorted(set(range(1, max(present, default=0) + 1)) - set(present)),
                     "expected_total": None})
    # Start with existing works lacking all episodes, followed by known internal gaps.
    rows.sort(key=lambda row: (row["recorded_episode_count"] != 0,
                               not bool(row["gaps_below_recorded_max"]), row["animation_id"]))
    return rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dump", type=Path, help="Use a backup instead of querying the live database")
    parser.add_argument("--properties", type=Path,
                        default=Path(__file__).resolve().parents[1] / "src/main/resources/application.properties")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    try:
        works, episodes = from_dump(args.dump) if args.dump else from_database(args.properties)
    except (ValueError, FileNotFoundError) as error:
        print(str(error), file=sys.stderr)
        return 1
    except Exception as error:
        # Database exception strings can contain usernames/hosts. Do not print them.
        code = error.args[0] if error.args and isinstance(error.args[0], int) else "n/a"
        print(f"Audit failed: {type(error).__name__}, code={code}. No database data was changed.", file=sys.stderr)
        return 1
    rows = inventory(works, episodes)
    report = {"source": str(args.dump.resolve()) if args.dump else "live database",
              "note": "Backup mode extracts row prefixes only. Names may contain original encoding damage. "
                      "Recorded URLs are not checked for availability; expected totals are unknown, so a continuous sequence does not prove completeness.",
              "summary": {"animation_rows": len(rows), "unique_animation_ids": len({r['animation_id'] for r in rows}),
                          "episode_rows": len(episodes),
                          "works_without_episodes": sum(not r["recorded_episodes"] for r in rows),
                          "works_with_internal_gaps": sum(bool(r["gaps_below_recorded_max"]) for r in rows)},
              "existing_works_only": rows}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report["summary"], ensure_ascii=False))
    print("Report saved to: " + str(args.output.resolve()))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
