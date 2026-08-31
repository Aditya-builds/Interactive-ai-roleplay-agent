import os
from functools import lru_cache


@lru_cache
def get_settings():
  return Settings()


class Settings:
  def __init__(self) -> None:
    self.host = os.getenv("VISUAL_AGENT_HOST", "0.0.0.0")
    self.port = int(os.getenv("VISUAL_AGENT_PORT", "8090"))
    self.log_level = os.getenv("VISUAL_AGENT_LOG_LEVEL", "INFO")
    self.langsmith_enabled = os.getenv("LANGSMITH_TRACING", "false").lower() == "true"
    self.langsmith_project = os.getenv("LANGSMITH_PROJECT", "visual-agent")
    self.use_llm = os.getenv("VISUAL_AGENT_USE_LLM", "false").lower() == "true"
    self.openai_api_key = os.getenv("OPENAI_API_KEY", "")
    self.openai_model = os.getenv("VISUAL_AGENT_LLM_MODEL", "gpt-4o-mini")
