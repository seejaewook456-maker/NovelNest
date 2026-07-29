import { RouterProvider } from 'react-router-dom';
import { router } from './router';
import SessionExpiredModal from './components/SessionExpiredModal';
import AppErrorBoundary from './components/AppErrorBoundary';
import './App.css';

function App() {
  return (
    <AppErrorBoundary>
      <RouterProvider router={router} />
      <SessionExpiredModal />
    </AppErrorBoundary>
  );
}

export default App;
