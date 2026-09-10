# Escrow-Flow Frontend

React SPA frontend for the Escrow-Flow milestone-based payment system.

## Tech Stack

- **React 18** with TypeScript
- **Redux Toolkit** for state management
- **React Router** for navigation
- **Vite** for blazing-fast build tooling
- **Axios** for API communication

## Getting Started

### Prerequisites

- Node.js 20+ and npm

### Installation

```bash
npm install
cp .env.example .env
# Configure VITE_API_URL in .env
npm run dev
```

The app will be available at `http://localhost:5173`

## Project Structure

```
src/
├── api/          # API client and endpoint functions
├── components/   # Reusable UI components
├── features/     # Redux slices and feature modules
├── pages/        # Route/page components
├── hooks/        # Custom React hooks
├── types/        # TypeScript type definitions
├── utils/        # Helper functions
└── App.tsx       # Root component with routing
```

## Available Scripts

- `npm run dev` - Start development server with hot reload
- `npm run build` - Build optimized production bundle
- `npm run preview` - Preview production build locally
- `npm run lint` - Run ESLint for code quality checks

## Environment Variables

Create a `.env` file in the frontend directory:

```env
VITE_API_URL=http://localhost:8080
```

- `VITE_API_URL` - Backend API base URL

## Features

- **Role-based UI**: Different views for clients and freelancers
- **Real-time updates**: Wallet balance and milestone status synchronization
- **Idempotency**: Generate UUID for fund-lock operations to prevent duplicate charges
- **JWT Authentication**: Secure token-based auth with automatic refresh
- **Responsive design**: Mobile-friendly interface

## Key Components

- **Wallet Dashboard**: Display user balance and recent transactions
- **Project Management**: Create and manage projects with milestones
- **Milestone Actions**: Lock funds (client), submit work (freelancer), approve/dispute (client)
- **Transaction History**: Complete audit trail of all wallet operations

## State Management

Redux Toolkit slices:
- `authSlice` - User authentication and JWT management
- `walletSlice` - Wallet balance and transactions
- `projectSlice` - Projects and milestones
- `uiSlice` - Loading states and notifications

## API Integration

All API calls go through centralized client (`src/api/client.ts`) with:
- Automatic JWT token injection
- Request/response interceptors
- Error handling and retry logic

See [API documentation](../docs/API.md) for endpoint details.

## Design Principles

- **Never compute balances client-side** - always fetch from backend
- **Validate state transitions server-side** - UI only presents allowed actions
- **Generate idempotency keys** for all payment operations
- **Refetch after mutations** to ensure UI reflects true server state

## Development Notes

Built with Vite for optimal developer experience:
- Hot Module Replacement (HMR)
- Fast cold starts
- Optimized builds with automatic code splitting

For architecture details and backend integration, see the [main documentation](../docs).
