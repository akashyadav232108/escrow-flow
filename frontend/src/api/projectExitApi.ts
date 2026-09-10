import api from './client';
import type { ProjectExitDetail, ResolveProjectExitInput } from '../types';

export const projectExitApi = {
  raise: (projectId: number, reason: string) =>
    api
      .post<ProjectExitDetail>(`/projects/${projectId}/exit`, { reason })
      .then((res) => res.data),

  getOpenForProject: (projectId: number) =>
    api.get<ProjectExitDetail>(`/projects/${projectId}/exit`).then((res) => res.data),

  getById: (exitId: number) =>
    api.get<ProjectExitDetail>(`/project-exits/${exitId}`).then((res) => res.data),
};

export const adminProjectExitApi = {
  list: (status?: string) =>
    api
      .get<ProjectExitDetail[]>('/admin/project-exits', {
        params: status ? { status } : undefined,
      })
      .then((res) => res.data),

  getById: (id: number) =>
    api.get<ProjectExitDetail>(`/admin/project-exits/${id}`).then((res) => res.data),

  resolve: (id: number, payload: ResolveProjectExitInput) =>
    api.post<ProjectExitDetail>(`/admin/project-exits/${id}/resolve`, payload).then((res) => res.data),
};
