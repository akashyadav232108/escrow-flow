import api from './client';
import type { ProjectAgreement } from '../types';

export const agreementApi = {
  get: (projectId: number) =>
    api.get<ProjectAgreement>(`/projects/${projectId}/agreement`).then((res) => res.data),

  accept: (projectId: number) =>
    api.post<ProjectAgreement>(`/projects/${projectId}/agreement/accept`).then((res) => res.data),
};

export const adminAgreementApi = {
  get: (projectId: number) =>
    api.get<ProjectAgreement>(`/admin/projects/${projectId}/agreement`).then((res) => res.data),
};
