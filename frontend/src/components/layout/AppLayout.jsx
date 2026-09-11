import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/useAuth';
import styles from './AppLayout.module.css';

const MENUS = [
  { to: '/dashboard', label: '대시보드' },
  { to: '/materials', label: '학습자료' },
  { to: '/tils', label: 'TIL' },
  { to: '/review', label: '복습' },
];

// '관련 학습' 은 TIL 상세 안의 기능이라 별도 화면이 없다. 비활성 메뉴로 남겨두면
// 영영 열리지 않는 메뉴가 되므로 지웠다.
const UPCOMING_MENUS = [];

export default function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    // 이동을 먼저 예약한다. logout() 을 먼저 부르면 RequireAuth 가 지금 화면을
    // "원래 가려던 위치"로 기록한 채 /login 으로 보내서, 다시 로그인했을 때
    // 대시보드가 아니라 로그아웃한 화면으로 돌아간다.
    navigate('/login', { replace: true });
    logout();
  };

  return (
    <div className={styles.shell}>
      <nav className={styles.nav}>
        <div className={styles.navInner}>
          <NavLink to="/materials" className={styles.brand}>
            TILink
          </NavLink>

          <div className={styles.menu}>
            {MENUS.map((menu) => (
              <NavLink
                key={menu.to}
                to={menu.to}
                className={({ isActive }) =>
                  `${styles.menuItem} ${isActive ? styles.menuItemActive : ''}`
                }
              >
                {menu.label}
              </NavLink>
            ))}
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
