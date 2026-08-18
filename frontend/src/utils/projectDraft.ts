export interface ProjectDraftMilestone {
  title: string;
  description: string;
  amount: string;
}

export interface ProjectDraft {
  title: string;
  description: string;
  milestones: ProjectDraftMilestone[];
  updatedAt: number;
}

export function getProjectDraftKey(userId: number): string {
  return `escrowflow:create-project-draft:${userId}`;
}

export function loadProjectDraft(userId: number): ProjectDraft | null {
  try {
    const raw = sessionStorage.getItem(getProjectDraftKey(userId));
    if (!raw) return null;
    const draft = JSON.parse(raw) as ProjectDraft;
    if (!draft || typeof draft.title !== 'string' || !Array.isArray(draft.milestones)) {
      return null;
    }
    return draft;
  } catch {
    return null;
  }
}

export function saveProjectDraft(
  userId: number,
  draft: Pick<ProjectDraft, 'title' | 'description' | 'milestones'>,
): void {
  sessionStorage.setItem(
    getProjectDraftKey(userId),
    JSON.stringify({ ...draft, updatedAt: Date.now() }),
  );
}

export function clearProjectDraft(userId: number): void {
  sessionStorage.removeItem(getProjectDraftKey(userId));
}

export function isProjectDraftDirty(
  draft: Pick<ProjectDraft, 'title' | 'description' | 'milestones'>,
): boolean {
  if (draft.title.trim() || draft.description.trim()) return true;
  return draft.milestones.some(
    (row) => row.title.trim() || row.description.trim() || row.amount.trim(),
  );
}

export function hasProjectDraft(userId: number): boolean {
  const draft = loadProjectDraft(userId);
  return draft !== null && isProjectDraftDirty(draft);
}
