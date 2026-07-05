import React, { useContext } from 'react';
import { AuthContext } from './context/AuthContext';
import LoginPortal from './views/LoginPortal';
import CitizenPortal from './views/CitizenPortal';
import MpDashboard from './views/MpDashboard';

function App() {
  const { user } = useContext(AuthContext);

  // If not logged in, redirect to login registration portal
  if (!user) {
    return <LoginPortal />;
  }

  // Route to target dashboard depending on user profile roles
  if (user.role === 'MP') {
    return <MpDashboard />;
  }

  return <CitizenPortal />;
}

export default App;
