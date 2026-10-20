import { NavLink } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function Navbar() {
  const { user, logout } = useAuth();
  if (!user) return null;

  return (
    <header className="navbar">
      <span className="brand">Expense Tracker</span>
      <nav>
        <NavLink to="/" end>
          Dashboard
        </NavLink>
        <NavLink to="/expenses">Expenses</NavLink>
        <NavLink to="/budgets">Budgets</NavLink>
      </nav>
      <div className="navbar-user">
        <span>{user.fullName}</span>
        <button className="link-button" onClick={logout}>
          Sign out
        </button>
      </div>
    </header>
  );
}
