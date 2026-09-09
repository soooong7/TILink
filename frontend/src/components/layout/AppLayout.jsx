import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/useAuth';
import styles from './AppLayout.module.css';

// 아직 만들지 않은 화면은 라우트를 임의로 만들지 않고 비활성 메뉴로만 보여준다.
const UPCOMING_MENUS = ['TIL', '관련 학습'];

export default function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className={styles.shell}>
      <nav className={styles.nav}>
        <div className={styles.navInner}>
          <NavLink to="/materials" className={styles.brand}>
            TILink
          </NavLink>

          <div className={styles.menu}>
            <NavLink
              to="/materials"
              className={({ isActive }) =>
                `${styles.menuItem} ${isActive ? styles.menuItemActive : ''}`
              }
            >
              학습자료
            </NavLink>
            {UPCOMING_MENUS.map((label) => (
              <span key={label} className={styles.menuItemDisabled} title="준비 중입니다">
                {label}
              </span>
            ))}
          </div>

          <div className={styles.account}>
            <span className={styles.userName}>{user?.name}</span>
            <button type="button" className={styles.logout} onClick={handleLogout}>
              로그아웃
            </button>
          </div>
        </div>
      </nav>

      <main className={styles.main}>
        <Outlet />
      </main>
    </div>
  );
}
