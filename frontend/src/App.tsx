import { RouterProvider } from 'react-router-dom';
import { router } from './router';
import SessionExpiredModal from './components/SessionExpiredModal';
import './App.css';

function App() {
  return (
    <>
      <RouterProvider router={router} />
      <SessionExpiredModal />
    </>
  );
}

export default App;
