import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { projectApi } from '../../api/projectApi';
import type { CreateProjectInput, Project } from '../../types';

interface ProjectsState {
  projects: Project[];
  selectedProject: Project | null;
  loading: boolean;
  error: string | null;
}

const initialState: ProjectsState = {
  projects: [],
  selectedProject: null,
  loading: false,
  error: null,
};

export const fetchProjects = createAsyncThunk('projects/fetchProjects', async (status?: string) =>
  projectApi.getProjects(status),
);

export const fetchProjectById = createAsyncThunk('projects/fetchProjectById', async (id: number) =>
  projectApi.getProjectById(id),
);

export const createProject = createAsyncThunk(
  'projects/createProject',
  async (payload: CreateProjectInput) => projectApi.createProject(payload),
);

export const acceptProject = createAsyncThunk('projects/acceptProject', async (id: number) =>
  projectApi.acceptProject(id),
);

export const lockFunds = createAsyncThunk(
  'projects/lockFunds',
  async ({ milestoneId, idempotencyKey }: { milestoneId: number; idempotencyKey: string }) =>
    projectApi.lockFunds(milestoneId, idempotencyKey),
);

export const submitMilestone = createAsyncThunk(
  'projects/submitMilestone',
  async ({ milestoneId, note }: { milestoneId: number; note: string }) =>
    projectApi.submitMilestone(milestoneId, note),
);

export const approveMilestone = createAsyncThunk(
  'projects/approveMilestone',
  async (milestoneId: number) => projectApi.approveMilestone(milestoneId),
);

export const disputeMilestone = createAsyncThunk(
  'projects/disputeMilestone',
  async ({ milestoneId, reason }: { milestoneId: number; reason?: string }) =>
    projectApi.disputeMilestone(milestoneId, reason),
);

const projectsSlice = createSlice({
  name: 'projects',
  initialState,
  reducers: {
    clearSelectedProject(state) {
      state.selectedProject = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchProjects.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchProjects.fulfilled, (state, action) => {
        state.loading = false;
        state.projects = action.payload;
      })
      .addCase(fetchProjects.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message ?? 'Failed to load projects';
      })
      .addCase(fetchProjectById.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchProjectById.fulfilled, (state, action) => {
        state.loading = false;
        state.selectedProject = action.payload;
      })
      .addCase(fetchProjectById.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message ?? 'Failed to load project';
      })
      .addCase(createProject.fulfilled, (state, action) => {
        state.projects.unshift(action.payload);
      })
      .addCase(acceptProject.fulfilled, (state, action) => {
        state.selectedProject = action.payload;
      });
  },
});

export const { clearSelectedProject } = projectsSlice.actions;
export default projectsSlice.reducer;
