from __future__ import annotations

import logging
import time

from fastapi import FastAPI

from app.config import get_settings
from app.graph.visual_graph import run_visual_graph
from app.models.visual_plan import VisualDirectorRequest, VisualDirectorResponse, VisualScenePlan

logging.basicConfig(level=get_settings().log_level)
logger = logging.getLogger("visual-agent")

app = FastAPI(title="Visual Agent", version="2.0.0")


@app.get("/health")
def health() -> dict[str, str]:
  return {"status": "ok"}


@app.post("/visual/plan", response_model=VisualDirectorResponse)
def create_visual_plan(request: VisualDirectorRequest) -> VisualDirectorResponse:
  started = time.perf_counter()
  logger.info(
    "visual_plan_start conversationId=%s focalCharacterId=%s explicitGeneration=%s messageCount=%s",
    request.conversationId,
    request.focalCharacterId,
    request.explicitGeneration,
    len(request.recentMessages),
  )
  plan = run_visual_graph(request)
  elapsed_ms = int((time.perf_counter() - started) * 1000)
  plan.graphExecutionMs = elapsed_ms
  logger.info(
    "visual_plan_complete conversationId=%s shouldGenerate=%s momentType=%s characters=%s promptLength=%s graphExecutionMs=%s",
    request.conversationId,
    plan.shouldGenerate,
    plan.momentType,
    len(plan.characters),
    len(plan.prompt),
    elapsed_ms,
  )
  return VisualDirectorResponse(plan=plan)
