import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { projectApi } from '../../api/projectApi';
import type { CreateProjectInput, Project } from '../../types';
import { extractApiErrorMessage } from '../../utils/errors';

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

export const fetchProjects = createAsyncThunk(
  'projects/fetchProjects',
  async (status: string | undefined, { rejectWithValue }) => {
    try {
      return await projectApi.getProjects(status);
    } catch (err) {
      return rejectWithValue(extractApiErrorMessage(err, 'Failed to load projects'));
    }
  },
);

export const fetchProjectById = createAsyncThunk(
  'projects/fetchProjectById',
  async (id: number, { rejectWithValue }) => {
    try {
      return await projectApi.getProjectById(id);
    } catch (err) {
      return rejectWithValue(extractApiErrorMessage(err, 'Failed to load project'));
    }
  },
);

export const createProject = createAsyncThunk(
  'projects/createProject',
  async (payload: CreateProjectInput, { rejectWithValue }) => {
    try {
      return await projectApi.createProject(payload);
    } catch (err) {
      return rejectWithValue(extractApiErrorMessage(err, 'Failed to create project'));
    }
  },
);

export const acceptProject = createAsyncThunk(
  'projects/acceptProject',
  async (id: number, { rejectWithValue }) => {
    try {
      return await projectApi.acceptProject(id);
    } catch (err) {
      return rejectWithValue(extractApiErrorMessage(err, 'Failed to accept project'));
    }
  },
);

export const lockFunds = createAsyncThunk(
  'projects/lockFunds',
  async (
    { milestoneId, idempotencyKey }: { milestoneId: number; idempotencyKey: string },
    { rejectWithValue },
  ) => {
    try {
      return await projectApi.lockFunds(milestoneId, idempotencyKey);
    } catch (err) {
      return rejectWithValue(extractApiErrorMessage(err, 'Failed to lock funds'));
    }
  },
);

export const submitMilestone = createAsyncThunk(
  'projects/submitMilestone',
  async ({ milestoneId, note }: { milestoneId: number; note: string }, { rejectWithValue }) => {
    try {
      return await projectApi.submitMilestone(milestoneId, note);
    } catch (err) {
      return rejectWithValue(extractApiErrorMessage(err, 'Failed to submit work'));
    }
  },
);

export const approveMilestone = createAsyncThunk(
  'projects/approveMilestone',
  async (milestoneId: number, { rejectWithValue }) => {
    try {
      return await projectApi.approveMilestone(milestoneId);
    } catch (err) {
      return rejectWithValue(extractApiErrorMessage(err, 'Failed to approve milestone'));
    }
  },
);

export const disputeMilestone = createAsyncThunk(
  'projects/disputeMilestone',
  async ({ milestoneId, reason }: { milestoneId: number; reason: string }, { rejectWithValue }) => {
    try {
      return await projectApi.disputeMilestone(milestoneId, reason);
    } catch (err) {
      return rejectWithValue(extractApiErrorMessage(err, 'Failed to dispute milestone'));
    }
  },
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
        state.error = (action.payload as string) ?? 'Failed to load projects';
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
        state.error = (action.payload as string) ?? 'Failed to load project';
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
