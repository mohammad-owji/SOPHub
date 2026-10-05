import MyInvitations from "./pages/MyInvitations";
import CreateProject from "./pages/CreateProject";
import Profile from "./pages/Profile";
import ProjectDetails from "./pages/ProjectDetails";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import Login from "./pages/Login";
import EmailBestaetigen from "./pages/EmailBestaetigen";
import Dashboard from "./pages/Dashboard";
import Projects from "./pages/Projects";
import ProjectInvitation from "./pages/ProjectInvitation";
import KI from "./pages/KI";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Login />} />
        {/* Registrierung = Login-Seite, die direkt im Registrieren-Modus startet */}
        <Route path="/register" element={<Login startModus="register" />} />
        {/* Link aus der Bestaetigungs-Mail */}
        <Route path="/email-bestaetigen" element={<EmailBestaetigen />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/projects" element={<Projects scope="alle" />} />
        <Route path="/my-projects" element={<Projects scope="meine" />} />
        <Route path="/projectdetails" element={<ProjectDetails />} />
        <Route path="/projectdetails/:id" element={<ProjectDetails />} />
        <Route path="/profile" element={<Profile />} />
        <Route path="/create-project" element={<CreateProject />} />
        <Route path="/einladungen" element={<MyInvitations />} />
        <Route path="/project-invitation/:projektId" element={<ProjectInvitation />} />
        <Route path="/ai" element={<KI />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;