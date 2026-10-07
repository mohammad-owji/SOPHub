import { Link, NavLink, useNavigate } from "react-router-dom";
import { getAuth } from "../services/api";
import "./Navbar.css";

// Lesbare Bezeichnung fuer die Rolle aus der Datenbank
const ROLLEN_NAME = {
    STUDENT: "Student:in",
    PROFESSOR: "Betreuer:in",
    ADMIN: "Admin",
};

// Hauptmenue (Reihenfolge = Reihenfolge in der Navbar)
// "Projekt erstellen" steht bewusst NICHT hier, sondern als Button auf
// Dashboard, "Meine Projekte" und "Alle Projekte" (keine doppelten Buttons).
const MENUE = [
    { ziel: "/dashboard", text: "Dashboard" },
    { ziel: "/my-projects", text: "Meine Projekte" },
    { ziel: "/projects", text: "Alle Projekte" },
    { ziel: "/einladungen", text: "Einladungen" },
    { ziel: "/ideas", text: "Projektideen" },
    { ziel: "/ai", text: "KI" },
];

function Navbar() {
    const navigate = useNavigate();
    const auth = getAuth();

    const handleLogout = () => {
        localStorage.removeItem("sophub_auth");
        navigate("/");
    };

    const initiale = auth?.vorname ? auth.vorname.charAt(0).toUpperCase() : "?";
    const rolle = ROLLEN_NAME[auth?.rolle] || auth?.rolle || "";

    return (
        <nav className="navbar navbar-expand-lg navbar-dark sop-navbar sticky-top">
            <div className="container-fluid px-4">

                {/* Logo */}
                <Link className="navbar-brand sop-brand" to="/dashboard">
                    <span className="sop-brand-logo">S</span>
                    <span>SOP<span className="sop-brand-akzent">hub</span></span>
                </Link>

                {/* Menue-Knopf fuer kleine Bildschirme (Handy) */}
                <button
                    className="navbar-toggler border-0"
                    type="button"
                    data-bs-toggle="collapse"
                    data-bs-target="#sopHauptmenue"
                    aria-controls="sopHauptmenue"
                    aria-expanded="false"
                    aria-label="Menü öffnen"
                >
                    <span className="navbar-toggler-icon"></span>
                </button>

                <div className="collapse navbar-collapse" id="sopHauptmenue">
                    <div className="navbar-nav mx-auto sop-nav-links">
                        {MENUE.map((eintrag) => (
                            <NavLink
                                key={eintrag.ziel}
                                className={({ isActive }) => `nav-link${isActive ? " active" : ""}`}
                                to={eintrag.ziel}
                            >
                                {eintrag.text}
                            </NavLink>
                        ))}
                    </div>

                    <div className="d-flex align-items-center gap-3">
                        <div className="dropdown">
                            <button
                                className="btn sop-benutzer dropdown-toggle d-flex align-items-center gap-2"
                                type="button"
                                data-bs-toggle="dropdown"
                                aria-expanded="false"
                            >
                                <span className="sop-avatar">{initiale}</span>

                                <span className="text-start d-none d-md-block lh-sm">
                                    <span className="d-block fw-semibold">{auth?.vorname || "Gast"}</span>
                                    <small className="sop-rolle">{rolle}</small>
                                </span>
                            </button>

                            <ul className="dropdown-menu dropdown-menu-end">
                                <li>
                                    <Link className="dropdown-item" to="/profile">
                                        Mein Profil
                                    </Link>
                                </li>

                                <li>
                                    <Link className="dropdown-item" to="/my-projects">
                                        Meine Projekte
                                    </Link>
                                </li>

                                <li>
                                    <Link className="dropdown-item" to="/einladungen">
                                        Einladungen
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
                </div>
            </div>
        </nav>
    );
}

export default Navbar;