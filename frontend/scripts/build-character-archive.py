"""Generate the site cast from a Bangumi Archive ZIP and a verified 0721 work export.

Usage from the repo root:
  python frontend/scripts/build-character-archive.py ARCHIVE.zip WORKS.json TSCharacters.txt

WORKS.json contains public catalog rows with type, id and title. Only structured
facts and source IDs are published. This generator never copies wiki prose or images.
"""
import argparse
from collections import Counter, defaultdict
import json
from pathlib import Path
import re
import unicodedata
import zipfile

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "frontend"
ARCHIVE_URL = "https://github.com/bangumi/Archive"
FORMATS = {"animation": 2, "comic": 1, "novel": 1, "galgame": 4}
LANGUAGE_EDITION = re.compile(r"[（(】\[\s]*(?:中国大陆配音版|普通话版|国语配音版|中文配音|国语版|中配版|粤语版|粤配版|日语版|日配版|原声版|中文|中配|国配|粤配)[）)】\]]*$")
SEASON = re.compile(r"第([一二三四五六七八九十0-9]+)[季期]")
SEASON_NUMBERS = dict(zip("一二三四五六七八九", map(str, range(1, 10)))) | {"十": "10"}


def load(path):
    return json.loads(Path(path).read_text(encoding="utf-8"))


def dictionary(path):
    entries = {}
    for line in Path(path).read_text(encoding="utf-8").splitlines():
        if "\t" in line and not line.startswith("#"):
            char, replacements = line.split("\t", 1)
            values = replacements.split()
            if len(char) == 1 and len(values) == 1:
                entries[ord(char)] = values[0]
    return entries


def name_keys(title, translation):
    if not title:
        return set()
    title = unicodedata.normalize("NFKC", title).translate(translation).casefold()
    title = SEASON.sub(lambda m: SEASON_NUMBERS.get(m[1], m[1]) + "期", title)
    title = re.sub(r"\b(\d+)(?:st|nd|rd|th)\s+season\s*$", r"\1期", title)
    title = re.sub(r"\bseason\s*(\d+)\s*$", r"\1期", title)
    key = "".join(ch for ch in title if ch.isalnum() or unicodedata.category(ch)[0] == "M")
    return {key} if key else set()


def wiki_aliases(infobox):
    aliases = []
    for match in re.finditer(r"(?m)^\|(?:别名|又名|译名)[^\S\r\n]*=[^\S\r\n]*(\{[^}]*\}|[^\r\n]*)", infobox):
        value = match[1].strip()
        if value.startswith("{"):
            aliases.extend(item.split("|")[-1].strip() for item in re.findall(r"\[([^\[\]]+)\]", value))
        else:
            aliases.extend(re.split(r"[、；;]", value))
    return [alias for alias in aliases if len(alias) >= 2 and not alias.startswith(("http", "@"))]


def fields(infobox, keys):
    regex = r"(?m)^\|(?:" + "|".join(map(re.escape, keys)) + r")[^\S\r\n]*=[^\S\r\n]*([^\r\n]+)"
    return [match[1].strip() for match in re.finditer(regex, infobox)
            if (match[1].strip() and not match[1].strip().startswith(("{", "[", "<", "|", "}")))]


def rows(archive, name):
    with archive.open(name + ".jsonlines") as source:
        for line in source:
            yield json.loads(line)


def work_kind(entry):
    if entry["type"] == 1:
        if entry["platform"] == 1001:
            return "comic"
        if entry["platform"] == 1002:
            return "novel"
        return None
    return {2: "animation", 4: "galgame"}.get(entry["type"])


def site_casts():
    catalog = load(BASE / "src/data/catalogCharacterAssociations.json")
    source = (BASE / "src/data/characterAssociations.ts").read_text(encoding="utf-8")
    for chunk in re.split(r"(?m)^  \{\s*$", source):
        work_matches = re.findall(r"\{ type: '(animation|comic|novel)', id: (\d+|UJI_ANIMATION_ID), title: '([^']+)'", chunk)
        people = re.findall(r"\{ id: (\d+|'[^']+'), name: '([^']+)'(?:, aliases: \[([^\]]*)\])?\s*\}", chunk)
        if work_matches:
            cast = [{"id": int(i) if i.isdecimal() else i[1:-1], "name": n,
                     "aliases": re.findall(r"'([^']+)'", aliases)} for i, n, aliases in people]
            catalog.append({"works": [{"type": kind, "id": int(i) if i.isdecimal() else 11187}
                                       for kind, i, _ in work_matches], "people": cast})
    result = defaultdict(list)
    for group in catalog:
        for work in group["works"]:
            result[work["type"], str(work["id"])].extend(group["people"])
    return result


def compact(values, limit):
    return list(dict.fromkeys(value for value in values if 0 < len(value) <= limit and not re.search(r"[<>\r\n]", value)))


def generate(archive, site, translation):
    by_key = defaultdict(list)
    for work in site:
        key = (work["type"], str(work["id"]))
        variants = [work["title"], LANGUAGE_EDITION.sub("", work["title"]), *work.get("aliases", [])]
        dubbed = variants[1]
        if " / " in dubbed and not re.search(r"第[一二三四五六七八九十0-9]+[季期]|[0-9]期|season", dubbed, re.I):
            variants += [part.strip() for part in dubbed.split(" / ")]
        if work["type"] == "novel":
            variants += [re.sub(r"(?:小说|小說)$", "", work["title"])]
        for name in variants:
            for normalized in name_keys(name, translation):
                by_key[(work["type"], normalized)].append(key)

    overrides = load(BASE / "scripts/character-archive-overrides.json")
    audited = {(work["type"], str(work["id"])): work["title"] for work in site}
    verified = defaultdict(list)
    for key, (title, subject_id) in overrides.items():
        kind, id = key.split(":", 1)
        if audited.get((kind, id)) == title:
            verified[subject_id].append((kind, id))
    candidates = defaultdict(dict)
    for subject in rows(archive, "subject"):
        kind = work_kind(subject)
        if not kind:
            continue
        for key in verified.get(subject["id"], ()):
            if kind == key[0]:
                candidates[key][subject["id"]] = 3
        primary = set().union(*(name_keys(n, translation) for n in (subject["name"], subject["name_cn"])))
        secondary = set().union(*(name_keys(n, translation) for n in wiki_aliases(subject["infobox"] or "")))
        for names, score in [(primary, 2), (secondary - primary, 1)]:
            for value in names:
                for key in by_key.get((kind, value), []):
                    current = candidates[key].get(subject["id"], 0)
                    candidates[key][subject["id"]] = max(current, score)

    subject_ids = {i for options in candidates.values() for i in options}
    roles = defaultdict(dict)
    for entry in rows(archive, "subject-characters"):
        if entry["subject_id"] in subject_ids and entry["type"] in (1, 2, 3):
            roles[entry["subject_id"]][entry["character_id"]] = (entry["type"], entry["order"])

    output_works = []
    unresolved = []
    for work in site:
        key = (work["type"], str(work["id"]))
        options = candidates.get(key, {})
        top_score = max(options.values(), default=0)
        ids = sorted(i for i, score in options.items() if score == top_score)
        if not ids:
            unresolved.append((*key, work["title"], "unmatched", ids))
            continue
        # Distinct entries with the same title may be seasons or remakes. Keep only
        # characters documented on every candidate instead of picking by popularity.
        common = set(roles[ids[0]])
        for i in ids[1:]:
            common.intersection_update(roles[i])
        if not common:
            unresolved.append((*key, work["title"], "no shared cast" if len(ids) > 1 else "no cast", ids))
            continue
        cast = []
        for char_id in sorted(common, key=lambda i: (*roles[ids[0]][i], i)):
            cast_types = {roles[i][char_id][0] for i in ids}
            cast.append([char_id, cast_types.pop() if len(cast_types) == 1 else 0])
        output_works.append({**{k: work[k] for k in ("type", "id", "title")}, "cast": cast, "sources": ids})

    character_ids = {c[0] for work in output_works for c in work["cast"]}
    characters = {}
    alternate_names = {}
    for character in rows(archive, "character"):
        if character["id"] not in character_ids or character["role"] != 1:
            continue
        info = character["infobox"] or ""
        native = character["name"].strip()
        local = compact(fields(info, ("简体中文名", "中文名", "繁体中文名")), 90)
        aliases = compact(wiki_aliases(info), 62)
        name = next(iter(local), native)
        if not name or len(name) > 90 or re.search(r"[<>\r\n]", name):
            continue
        birth = compact(fields(info, ("生日", "诞生日")), 45)
        entry = {"id": character["id"], "name": name}
        if native != name:
            entry["originalName"] = native
        if birth:
            entry["birthday"] = birth[0]
        facts = []
        for label in ("性别", "身高", "所属", "职业"):
            values = compact(fields(info, (label,)), 48)
            if values:
                facts.append([label, values[0]])
        if facts:
            entry["facts"] = facts
        alternatives = [a for a in [*local, *aliases] if a not in {name, native}]
        if alternatives:
            entry["aliases"] = alternatives[:5]
        characters[character["id"]] = entry
        alternate_names[character["id"]] = set().union(*(name_keys(a, translation) for a in [name, native, *alternatives]))

    canonical = defaultdict(set)
    registry = site_casts()
    game_catalog = {g["slug"]: g["characters"] for g in load(BASE / "src/data/galgameMedia.json")}
    for game_slug, people in game_catalog.items():
        registry["galgame", game_slug] = [{"id": f"{game_slug}:{row['name']}", "name": row["name"],
                                          "aliases": [row.get("originalName", "")], "gameSlug": game_slug} for row in people]
    for work in output_works:
        key = (work["type"], str(work["id"]))
        people = registry.get(key, [])
        names = [(person["id"], set().union(*(name_keys(n, translation) for n in [person["name"], *person.get("aliases", [])])))
                 for person in people]
        for char_id, _ in work["cast"]:
            matching = [site_id for site_id, keys in names if keys & alternate_names.get(char_id, set())]
            canonical[char_id].update(matching)
    for char_id, ids in canonical.items():
        editorial = [i for i in ids if ":" not in str(i)]
        if len(editorial) == 1:
            characters[char_id]["siteId"] = editorial[0]
        elif not editorial and len(ids) == 1:
            slug, name = str(next(iter(ids))).split(":", 1)
            characters[char_id]["game"] = [slug, name]

    valid_sources = defaultdict(set)
    for work in output_works:
        work["cast"] = [c for c in work["cast"] if c[0] in characters]
        if len(work["sources"]) == 1:
            valid_sources[work["sources"][0]].update(c[0] for c in work["cast"])
    voice_ids = defaultdict(set)
    for entry in rows(archive, "person-characters"):
        if entry["character_id"] in valid_sources.get(entry["subject_id"], ()):
            voice_ids[(entry["subject_id"], entry["character_id"])].add(entry["person_id"])
    needed_actors = set().union(*voice_ids.values()) if voice_ids else set()
    actors = {}
    for actor in rows(archive, "person"):
        if actor["id"] in needed_actors and actor["name"] and len(actor["name"]) <= 80:
            actors[actor["id"]] = actor["name"]
    for work in output_works:
        if len(work["sources"]) != 1:
            continue
        subject = work["sources"][0]
        for char in work["cast"]:
            names = sorted({actors[i] for i in voice_ids.get((subject, char[0]), []) if i in actors})
            if names:
                char.append(names)
    result = [w for w in output_works if w["cast"]]
    return {"source": ARCHIVE_URL, "people": list(characters.values()), "works": result}, unresolved



def write_shards(result, target):
    target.mkdir(parents=True, exist_ok=True)
    people = {row["id"]: row for row in result["people"]}
    appearances = defaultdict(list)
    links = defaultdict(dict)
    cast_buckets = [dict() for _ in range(16)]
    profile_buckets = [dict() for _ in range(16)]

    def write(name, data):
        (target / (name + ".json")).write_text(
            json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")

    for work in result["works"]:
        key = f"{work['type']}:{work['id']}"
        bucket = sum(ord(char) for char in key) % len(cast_buckets)
        following = cast_buckets[bucket][key] = []
        for role in work["cast"]:
            person = people[role[0]]
            following.append([{"id": person["id"], "name": person["name"],
                               **{k: person[k] for k in ("originalName", "siteId", "game") if k in person}}, role[1]])
            detail = {**work, "cast": [role]}
            appearances[person["id"]].append(detail)
            if "siteId" in person:
                links[str(person["siteId"])][key] = {**work, "cast": []}
    search = []
    for id, works in appearances.items():
        person = people[id]
        profile_buckets[id % len(profile_buckets)][str(id)] = {"person": person, "works": works}
        if "siteId" not in person and "game" not in person:
            search.append([id, person["name"], works[0]["title"], person.get("originalName", ""), *person.get("aliases", [])])

    for i, (works, profiles) in enumerate(zip(cast_buckets, profile_buckets)):
        write(f"work-{i}", works)
        write(f"profile-{i}", profiles)
    write("search", search)
    write("links", {person: list(works.values()) for person, works in links.items()})

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive")
    parser.add_argument("works")
    parser.add_argument("dictionary")
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    site = load(args.works)
    for game in load(ROOT / "backend/data/galgame_seed.json"):
        site.append({"type": "galgame", "id": game["slug"], "title": game["name_zh"],
                     "aliases": [game.get("name_original", ""), *re.split(r"\s+/\s+", game.get("aliases", "") or "")]})
    with zipfile.ZipFile(args.archive) as archive:
        result, unresolved = generate(archive, site, dictionary(args.dictionary))
    target = BASE / "public/data/characters"
    write_shards(result, target)
    if args.report:
        args.report.write_text(json.dumps(unresolved, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Covered", dict(Counter(work["type"] for work in result["works"])))
    print("People", len(result["people"]), "Archive unresolved", len(unresolved), "Bytes", sum(p.stat().st_size for p in target.glob("*.json")))


if __name__ == "__main__":
    main()
