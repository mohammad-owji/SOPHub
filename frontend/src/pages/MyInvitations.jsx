import { useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";


function MyInvitations() {

    const navigate = useNavigate();


    const testInvitation = {
        id: 4,
        titel: "SOPhub Testprojekt",
        fachbereich: "Informatik",
        projektart: "Softwareprojekt",
        semester: "SoSe 2026"
    };



    const openInvitation = () => {

        navigate(
            `/project-invitation/${testInvitation.id}`
        );

    };



    return (

        <div>

            <Navbar />


            <div className="container mt-5">


                <h2 className="fw-bold mb-4">
                    Meine Einladungen
                </h2>



                <div
                    className="card shadow-sm border-0"
                    style={{ maxWidth: "700px" }}
                >

                    <div className="card-body p-4">


                        <h4 className="fw-bold">
                            {testInvitation.titel}
                        </h4>



                        <hr />


                        <div className="mb-2">

                            <strong>
                                Fachbereich:
                            </strong>

                            <span className="text-muted ms-2">
                                {testInvitation.fachbereich}
                            </span>

                        </div>



                        <div className="mb-2">

                            <strong>
                                Projektart:
                            </strong>

                            <span className="text-muted ms-2">
                                {testInvitation.projektart}
                            </span>

                        </div>




                        <div className="mb-3">

                            <strong>
                                Semester:
                            </strong>

                            <span className="text-muted ms-2">
                                {testInvitation.semester}
                            </span>

                        </div>




                        <button
                            className="btn btn-primary"
                            onClick={openInvitation}
                        >
                            Anfrage öffnen
                        </button>



                    </div>


                </div>


            </div>


        </div>

    );
}


export default MyInvitations;