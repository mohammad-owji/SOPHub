import CreateProject from "./pages/CreateProject";
import Profile from "./pages/Profile";
import ProjectDetails from "./pages/ProjectDetails";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import Projects from "./pages/Projects";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/projects" element={<Projects />} />
        <Route path="/projectdetails" element={<ProjectDetails />} />
        <Route path="/profile" element={<Profile />} />
        <Route path="/create-project" element={<CreateProject />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;