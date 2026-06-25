import { Link, NavLink, useNavigate } from "react-router-dom";

function Navbar() {
    const navigate = useNavigate();

    const handleLogout = () => {
        navigate("/");
    };

    const navLinkClass = ({ isActive }) =>
        isActive ? "nav-link active fw-semibold" : "nav-link";

    return (
        <nav
            className="navbar navbar-expand navbar-dark shadow-sm"
            style={{ backgroundColor: "#00a7ef" }}
        >
            <div className="container-fluid px-4">
                <Link className="navbar-brand fw-bold fs-4" to="/dashboard">
                    SOPhub
                </Link>

                <div className="navbar-nav mx-auto">
                    <NavLink className={navLinkClass} to="/dashboard">
                        Dashboard
                    </NavLink>

                    <NavLink className={navLinkClass} to="/my-projects">
                        Meine Projekte
                    </NavLink>

                    <NavLink className={navLinkClass} to="/projects">
                        Alle Projekte
                    </NavLink>

                    <NavLink className={navLinkClass} to="/ideas">
                        Projektideen
                    </NavLink>

                    <NavLink className={navLinkClass} to="/create-project">
                        Projekt erstellen
                    </NavLink>

                    <NavLink className={navLinkClass} to="/ai">
                        KI
                    </NavLink>
                </div>

                <div className="dropdown">
                    <button
                        className="btn text-white dropdown-toggle d-flex align-items-center gap-2 border-0"
                        type="button"
                        data-bs-toggle="dropdown"
                        aria-expanded="false"
                    >
                        <div
                            className="rounded-circle bg-white text-primary d-flex align-items-center justify-content-center fw-bold"
                            style={{ width: "38px", height: "38px" }}
                        >
                            S
                        </div>

                        <div className="text-start d-none d-md-block">
                            <div className="fw-semibold">Salma</div>
                            <small className="text-white-50">Studentin</small>
                        </div>
                    </button>

                    <ul className="dropdown-menu dropdown-menu-end shadow border-0">
                        <li>
                            <Link className="dropdown-item" to="/profile">
                                Profil bearbeiten
                            </Link>
                        </li>

                        <li>
                            <Link className="dropdown-item" to="/my-projects">
                                Meine Projekte
                            </Link>
                        </li>

                        <li>
                            <hr className="dropdown-divider" />
                        </li>

                        <li>
                            <button
                                className="dropdown-item text-danger"
                                onClick={handleLogout}
                            >
                                Logout
                            </button>
                        </li>
                    </ul>
                </div>
            </div>
        </nav>
    );
}

export default Navbar;