from __future__ import annotations

from langgraph.graph import END, START, StateGraph

from app.graph.nodes.character_selector import select_characters
from app.graph.nodes.consistency_guard import consistency_guard
from app.graph.nodes.context_selector import select_relevant_context
from app.graph.nodes.interaction_director import direct_character_interaction
from app.graph.nodes.prompt_compiler import compile_visual_prompt
from app.graph.nodes.scene_analyzer import analyze_scene, detect_visual_moment
from app.graph.nodes.scene_composer import compose_scene
from app.graph.nodes.visual_identity_retriever import retrieve_visual_identities
from app.graph.state import VisualGraphState
from app.models.visual_plan import SceneComposition, VisualDirectorRequest, VisualScenePlan


def _route_after_moment(state: VisualGraphState) -> str:
  if state.get("should_generate"):
    return "continue"
  return "stop"


def build_visual_graph():
  graph = StateGraph(VisualGraphState)
  graph.add_node("analyze_scene", analyze_scene)
  graph.add_node("detect_visual_moment", detect_visual_moment)
  graph.add_node("select_characters", select_characters)
  graph.add_node("retrieve_visual_identities", retrieve_visual_identities)
  graph.add_node("select_relevant_context", select_relevant_context)
  graph.add_node("direct_character_interaction", direct_character_interaction)
  graph.add_node("compose_scene", compose_scene)
  graph.add_node("consistency_guard", consistency_guard)
  graph.add_node("compile_visual_prompt", compile_visual_prompt)

  graph.add_edge(START, "analyze_scene")
  graph.add_edge("analyze_scene", "detect_visual_moment")
  graph.add_conditional_edges(
    "detect_visual_moment",
    _route_after_moment,
    {
      "continue": "select_characters",
      "stop": END,
    },
  )
  graph.add_edge("select_characters", "retrieve_visual_identities")
  graph.add_edge("retrieve_visual_identities", "select_relevant_context")
  graph.add_edge("select_relevant_context", "direct_character_interaction")
  graph.add_edge("direct_character_interaction", "compose_scene")
  graph.add_edge("compose_scene", "consistency_guard")
  graph.add_edge("consistency_guard", "compile_visual_prompt")
  graph.add_edge("compile_visual_prompt", END)
  return graph.compile()


def run_visual_graph(request: VisualDirectorRequest) -> VisualScenePlan:
  app = build_visual_graph()
  initial_state: VisualGraphState = {"request": request}
  final_state = app.invoke(initial_state)

  if not final_state.get("should_generate"):
    analysis = final_state.get("scene_analysis")
    return VisualScenePlan(
      shouldGenerate=False,
      momentType=analysis.momentType if analysis else "CONVERSATION",
      reasoningSummary="Visual moment not important enough for generation.",
      scene=SceneComposition(),
    )

  scene = final_state.get("scene_composition", SceneComposition())
  return VisualScenePlan(
    shouldGenerate=True,
    momentType=final_state.get("scene_analysis").momentType if final_state.get("scene_analysis") else "CONVERSATION",
    reasoningSummary=final_state.get("reasoning_summary", ""),
    characters=final_state.get("plan_characters", []),
    scene=scene,
    interaction=final_state.get("interaction_plan"),
    prompt=final_state.get("visual_prompt", ""),
    negativePrompt=final_state.get("negative_prompt", ""),
  )
