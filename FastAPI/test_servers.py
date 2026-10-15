"""Run with: python -m unittest discover -s FastAPI (no model download required)."""
import asyncio
import importlib.util
import os
from pathlib import Path
import sys
import tempfile
import types
import unittest
from unittest.mock import Mock, patch

from fastapi import HTTPException


class ServerContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.servers = {}
        with tempfile.NamedTemporaryFile(suffix=".gguf") as model:
            for name in ("llamaServer", "deepseekServer"):
                fake_llama = types.ModuleType("llama_cpp")
                fake_llama.Llama = Mock(return_value=Mock())
                with patch.dict(sys.modules, {"llama_cpp": fake_llama}), patch.dict(os.environ, {
                    "LLAMA_MODEL_PATH": model.name, "DEEPSEEK_MODEL_PATH": model.name,
                }):
                    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(name + ".py"))
                    module = importlib.util.module_from_spec(spec)
                    spec.loader.exec_module(module)
                    cls.servers[name] = module

    def test_routes_and_request_defaults(self):
        for name, server in self.servers.items():
            with self.subTest(server=name):
                values = {"message": "질문"}
                if name == "llamaServer":
                    values["sessionId"] = "user@example.test"
                first, second = server.ChatRequest(**values), server.ChatRequest(**values)
                first.history.append({"user": "previous"})
                self.assertEqual(second.history, [])
                paths = server.app.openapi()["paths"]
                self.assertIn("/api/fastapi/chat" if name == "llamaServer" else "/chat", paths)
                if name == "llamaServer":
                    self.assertIn("/api/fastapi/health", paths)
                    self.assertEqual(server.health()["status"], "ok")

    def test_llama_history_limit_and_response(self):
        server = self.servers["llamaServer"]
        server.llm.create_chat_completion.return_value = {"choices": [{"message": {"content": " 답변 "}}]}
        request = server.ChatRequest(message="현재 질문", sessionId="user@example.test",
                                     history=[{"user": str(i), "assistant": "answer"} for i in range(6)])
        self.assertEqual(asyncio.run(server.chat(request)), {"response": "답변"})
        messages = server.llm.create_chat_completion.call_args.kwargs["messages"]
        self.assertEqual(messages[0]["role"], "system")
        self.assertEqual(messages[1], {"role": "user", "content": "2"})
        self.assertEqual(messages[-1], {"role": "user", "content": "현재 질문"})
        self.assertEqual(len(messages), 10)

    def test_deepseek_history_prompt_and_response(self):
        server = self.servers["deepseekServer"]
        server.llm.create_completion.return_value = {"choices": [{"text": " 답변 "}]}
        request = server.ChatRequest(message="질문", history=[{"user": "이전 질문", "assistant": "이전 답변"}])
        self.assertEqual(asyncio.run(server.chat(request)), {"response": "답변"})
        self.assertEqual(server.llm.create_completion.call_args.kwargs["prompt"],
                         "Human: 이전 질문\nAssistant: 이전 답변\n\nHuman: 질문\n\nAssistant:")

    def test_generation_failure_remains_http_500(self):
        for name, server in self.servers.items():
            with self.subTest(server=name):
                method = server.llm.create_chat_completion if name == "llamaServer" else server.llm.create_completion
                method.side_effect = RuntimeError("model unavailable")
                values = {"message": "질문", "sessionId": "user@example.test"}
                with self.assertRaises(HTTPException) as failure:
                    asyncio.run(server.chat(server.ChatRequest(**values)))
                self.assertEqual(failure.exception.status_code, 500)
                method.side_effect = None


if __name__ == "__main__":
    unittest.main()
