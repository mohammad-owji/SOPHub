import MyInvitations from "./pages/MyInvitations";
import CreateProject from "./pages/CreateProject";
import Profile from "./pages/Profile";
import ProjectDetails from "./pages/ProjectDetails";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import Projects from "./pages/Projects";
import ProjectInvitation from "./pages/ProjectInvitation";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/projects" element={<Projects scope="alle" />} />
        <Route path="/my-projects" element={<Projects scope="meine" />} />
        <Route path="/projectdetails" element={<ProjectDetails />} />
        <Route path="/projectdetails/:id" element={<ProjectDetails />} />
        <Route path="/profile" element={<Profile />} />
        <Route path="/create-project" element={<CreateProject />} />
        <Route path="/einladungen" element={<MyInvitations />} />
        <Route path="/project-invitation/:projektId" element={<ProjectInvitation />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
