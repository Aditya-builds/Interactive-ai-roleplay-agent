from __future__ import annotations

from app.graph.state import VisualGraphState


def compile_visual_prompt(state: VisualGraphState) -> VisualGraphState:
  identities = {identity.characterId: identity for identity in state.get("selected_identities", [])}
  scene = state["scene_composition"]
  interaction = state["interaction_plan"]
  context = state["visual_context"]
  analysis = state.get("scene_analysis")

  reference_lines: list[str] = []
  identity_lines: list[str] = []
  interaction_lines: list[str] = []
  pose_lines: list[str] = []
  clothing_lines: list[str] = []
  negative_parts: list[str] = [
    "different face",
    "different facial structure",
    "different eye color",
    "different hair color",
    "different hairstyle",
    "different skin tone",
    "different body proportions",
    "inconsistent character",
    "duplicate characters",
    "blurry",
    "low quality",
    "watermark",
    "text",
  ]

  for character in state.get("plan_characters", []):
    identity = identities.get(character.characterId)
    ref = character.referenceImage or (identity.canonicalReferenceImage if identity else None)
    if ref:
      reference_lines.append(f"- {character.name} ({character.characterId}): {ref}")
    if identity:
      identity_lines.append(
        f"{character.name}:\n"
        f"  Overall: {identity.visualDescription or character.name}\n"
        f"  Face: {identity.faceDescription or 'match canonical reference'}\n"
        f"  Hair: {identity.hairDescription or 'match canonical reference'}\n"
        f"  Eyes: {identity.eyeDescription or 'match canonical reference'}\n"
        f"  Skin: {identity.skinDescription or 'match canonical reference'}\n"
        f"  Body: {identity.bodyDescription or 'match canonical reference'}"
      )
      if identity.negativePrompt:
        negative_parts.append(identity.negativePrompt)
    pose_lines.append(
      f"{character.name}: pose={character.pose}; expression={character.expression}; action={character.action}; position={character.position}"
    )
    clothing_lines.append(
      f"{character.name}: {character.sceneClothing or 'canonical outfit from reference image unless scene explicitly changed clothing'}"
    )

  for item in interaction.characters:
    interaction_lines.append(
      f"{item.name}: position={item.position}; gaze={item.gaze}; action={item.action}"
    )

  prompt = f"""
CHARACTER REFERENCES
Use canonical reference images as the source of truth for identity.
{chr(10).join(reference_lines)}

CHARACTER IDENTITY
Preserve face, hair, eyes, skin tone, and body proportions exactly.
{chr(10).join(identity_lines)}

CHARACTER INTERACTIONS
Focus: {interaction.focus}
Distance: {interaction.distance}
Body language: {interaction.bodyLanguage}
Emotional tension: {interaction.emotionalTension}
{chr(10).join(interaction_lines)}

POSES AND EXPRESSIONS
{chr(10).join(pose_lines)}

CLOTHING
{chr(10).join(clothing_lines)}

ENVIRONMENT
Location: {scene.location} ({scene.locationDescription})
Time: {scene.time}
Environment: {scene.environment}

CAMERA
{scene.camera}
Framing: {scene.framing}
Composition: {scene.composition}

LIGHTING
{scene.lighting}

ATMOSPHERE
{scene.atmosphere}

RECENT VISUAL MOMENT
{context.recentMoment}

RELATIONSHIP CONTEXT
{context.relationshipNotes}

ART STYLE
{next((identities[c.characterId].artStyle for c in state.get('plan_characters', []) if c.characterId in identities and identities[c.characterId].artStyle), 'dark fantasy cinematic illustration')}

CONSISTENCY REQUIREMENTS
- Preserve each referenced character identity exactly.
- Do not alter facial identity, hair color, eye color, or skin tone.
- Render the requested multi-character interaction, not isolated unrelated portraits.
- Reflect the recent visual moment and spatial relationships.
""".strip()

  reasoning = (
    f"moment={analysis.momentType if analysis else 'UNKNOWN'}; "
    f"importance={analysis.visualImportance if analysis else 'UNKNOWN'}; "
    f"characters={len(state.get('plan_characters', []))}"
  )
  return {
    **state,
    "visual_prompt": prompt,
    "negative_prompt": ", ".join(dict.fromkeys(negative_parts)),
    "reasoning_summary": reasoning,
  }
