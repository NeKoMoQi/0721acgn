import copy
import json
from pathlib import Path
import struct
import sys
import tempfile
import unittest
from unittest.mock import MagicMock, Mock, patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import ingest_from_netdisk as flow


def job():
    return {"animation_id": 10941, "animation_name": "葬送的芙莉莲", "episode_no": 4,
            "netdisk_path": "/0721acgn-ingest/04.mp4", "expected_size": 100,
            "subtitle_language": "zh-Hant", "subtitle_kind": "burned_in", "subtitles_verified": False}


def info():
    return {"format": "mov,mp4,m4a,3gp,3g2,mj2", "duration": 1470,
            "video": {"codec_name": "h264", "pix_fmt": "yuv420p", "width": 1920, "height": 1080},
            "audio": {"codec_name": "aac"}}


def atom(kind, body=b""):
    return struct.pack(">I4s", 8 + len(body), kind) + body


class ManifestTests(unittest.TestCase):
    def test_disabled_unconfigured_job_is_ignored(self):
        self.assertEqual(flow.validate_jobs({"jobs": [{"enabled": False}]}), [])

    def test_no_duplicate_episode_or_guessed_size(self):
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            flow.validate_jobs({"jobs": [job(), job()]})
        entry = job()
        entry["expected_size"] = None
        with self.assertRaisesRegex(ValueError, "expected_size"):
            flow.validate_jobs({"jobs": [entry]})

    def test_soft_subtitles_are_not_silently_lost(self):
        entry = job()
        entry["subtitle_kind"] = "soft"
        with self.assertRaisesRegex(ValueError, "burned-in"):
            flow.validate_jobs({"jobs": [entry]})

    def test_remote_traversal_rejected(self):
        entry = job()
        entry["netdisk_path"] = "/../04.mp4"
        with self.assertRaisesRegex(ValueError, "absolute video"):
            flow.validate_jobs({"jobs": [entry]})


class MediaTests(unittest.TestCase):
    def test_web_compatible_video_and_rejected_codecs(self):
        self.assertTrue(flow.compatible(info()))
        for field, value in (("codec_name", "hevc"), ("pix_fmt", "yuv420p10le"), ("width", 3840)):
            value_info = info()
            value_info["video"][field] = value
            self.assertFalse(flow.compatible(value_info))
        value_info = info()
        value_info["audio"]["codec_name"] = "opus"
        self.assertFalse(flow.compatible(value_info))

    def test_faststart_atom_order_and_malformed_atoms(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "sample.mp4"
            path.write_bytes(atom(b"ftyp") + atom(b"moov") + atom(b"mdat", b"1234"))
            self.assertTrue(flow.faststart(path))
            path.write_bytes(atom(b"ftyp") + atom(b"mdat", b"1234") + atom(b"moov"))
            self.assertFalse(flow.faststart(path))
            path.write_bytes(struct.pack(">I4s", 1, b"moov"))
            self.assertFalse(flow.faststart(path))

    def test_compatible_faststart_file_is_returned_without_encoding(self):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            path = folder / "04.mp4"
            path.write_bytes(atom(b"moov") + atom(b"mdat"))
            with patch.object(flow, "run_tool") as tool:
                ready, action = flow.prepare(path, info(), folder)
            self.assertEqual(ready, path)
            self.assertEqual(action, "copy-original")
            tool.assert_not_called()

    def test_tail_moov_is_remuxed_without_reencoding(self):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            source = folder / "04.mp4"
            source.write_bytes(atom(b"mdat") + atom(b"moov"))
            def execute(command, **kwargs):
                Path(command[-1]).write_bytes(atom(b"moov") + atom(b"mdat"))
            with patch.object(flow, "run_tool", side_effect=execute) as tool, patch.object(flow, "probe", return_value=info()):
                ready, action = flow.prepare(source, info(), folder)
            self.assertEqual(action, "remux")
            self.assertTrue(ready.exists())
            command = tool.call_args.args[0]
            self.assertIn("copy", command)
            self.assertNotIn("libx264", command)

    def test_probe_rejects_multiple_audio_tracks(self):
        data = {"format": {"duration": "1470"}, "streams": [
            {"codec_type": "video", "width": 1920, "height": 1080},
            {"codec_type": "audio"}, {"codec_type": "audio"}]}
        with patch.object(flow, "run_tool", return_value=json.dumps(data).encode()):
            with self.assertRaisesRegex(ValueError, "one audio"):
                flow.probe(Path("04.mp4"))


class DownloadTests(unittest.TestCase):
    def test_zero_exit_without_completed_file_is_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            with patch.object(flow, "run_tool", return_value=None):
                with self.assertRaisesRegex(ValueError, "missing/size"):
                    flow.download(job(), {"pcs_executable": "fake.exe"}, Path(directory) / "10941-4")

    def test_wrong_size_file_is_never_overwritten(self):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            path = folder / "04.mp4"
            path.write_bytes(b"partial")
            with patch.object(flow, "run_tool") as tool:
                with self.assertRaisesRegex(ValueError, "missing/size"):
                    flow.download(job(), {}, folder)
            self.assertEqual(path.read_bytes(), b"partial")
            tool.assert_not_called()


class DatabaseTests(unittest.TestCase):
    def test_existing_episode_prevents_all_download_and_cloud_work(self):
        driver = MagicMock()
        with tempfile.TemporaryDirectory() as directory:
            with patch.dict("sys.modules", {"pymysql": driver}), patch.object(flow, "database_config", return_value={}), \
                    patch.object(flow, "existing", return_value=("https://example.org/already.mp4",)), \
                    patch.object(flow, "download") as download, patch.object(flow, "publish") as publish:
                flow.process([("10941-4", job())], {}, Path("unused"), Path(directory))
            download.assert_not_called()
            publish.assert_not_called()

    def test_concurrent_different_url_is_preserved(self):
        db = MagicMock()
        cursor = db.cursor.return_value.__enter__.return_value
        cursor.fetchone.return_value = ("https://example.org/other.mp4",)
        with self.assertRaisesRegex(ValueError, "another process"):
            flow.register(db, job(), "https://example.org/new.mp4")
        db.rollback.assert_called_once()
        db.commit.assert_not_called()
        self.assertEqual(cursor.execute.call_count, 1)

    def test_database_failure_rolls_back_without_marking_completed(self):
        db = MagicMock()
        cursor = db.cursor.return_value.__enter__.return_value
        cursor.fetchone.return_value = None
        cursor.execute.side_effect = [None, RuntimeError("simulated write failure")]
        with self.assertRaises(RuntimeError):
            flow.register(db, job(), "https://example.org/new.mp4")
        db.rollback.assert_called_once()
        db.commit.assert_not_called()


class ResumeTests(unittest.TestCase):
    def test_uploaded_object_is_reused_for_database_retry(self):
        with tempfile.TemporaryDirectory() as directory:
            work = Path(directory)
            ready = work / "04.mp4"
            ready.write_bytes(b"verified-source")
            item = {"uploaded": True, "output_size": ready.stat().st_size, "output_sha256": flow.digest(ready),
                    "url": "https://example.org/existing.mp4"}
            state = {"10941-4": item}
            with patch.dict("sys.modules", {"oss2": None}), patch.object(flow, "verify_public") as verify, \
                    patch.object(flow, "register") as register:
                flow.publish(Mock(), job(), ready, {}, Path("unused"), item, work, state)
            verify.assert_called_once()
            register.assert_called_once()
            self.assertEqual(item["status"], "completed")

    def test_download_review_then_publish_reuses_prepared_file(self):
        with tempfile.TemporaryDirectory() as directory:
            work = Path(directory)
            folder = work / "10941-4"
            folder.mkdir()
            source = folder / "04.mp4"
            source.write_bytes(b"source")
            config = {"min_free_gb": 0}
            entry = job()
            with patch.object(flow, "download", return_value=source), patch.object(flow, "inspect", return_value=info()):
                flow.process([("10941-4", entry)], config, Path("unused"), work, download_only=True)
            # Approval changes only; the source identity must remain valid.
            entry["subtitles_verified"] = True
            driver = MagicMock()
            with patch.dict("sys.modules", {"pymysql": driver}), patch.object(flow, "database_config", return_value={}), \
                    patch.object(flow, "existing", return_value=None), patch.object(flow, "download", return_value=source), \
                    patch.object(flow, "inspect", return_value=info()), patch.object(flow, "prepare", return_value=(source, "copy-original")) as prepare, \
                    patch.object(flow, "publish"):
                flow.process([("10941-4", entry)], config, Path("unused"), work)
                flow.process([("10941-4", entry)], config, Path("unused"), work)
            self.assertEqual(prepare.call_count, 1)


if __name__ == "__main__":
    unittest.main()
