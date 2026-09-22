import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import Navbar from "../components/Navbar";
import {
    acceptProject,
    declineProject,
    getProjectById
} from "../services/api";


const CURRENT_USER_ID = 3;


function ProjectInvitation() {

    const { projektId } = useParams();

    const [projekt, setProjekt] = useState(null);
    const [decision, setDecision] = useState("");
    const [error, setError] = useState("");



    useEffect(() => {

        async function loadProject() {

            try {

                const data = await getProjectById(projektId);
                setProjekt(data);

            } catch (err) {

                console.error(err);

                setError(
                    "Projekt konnte nicht geladen werden."
                );

            }
        }


        loadProject();

    }, [projektId]);






    const handleAccept = async () => {

        try {

            const updatedProjekt = await acceptProject(
                projektId,
                CURRENT_USER_ID
            );


            setProjekt(updatedProjekt);
            setDecision("accepted");


        } catch (err) {

            console.error(err);

            setError(
                err.message
            );

        }
    };







    const handleDecline = async () => {

        try {

            const updatedProjekt = await declineProject(
                projektId,
                CURRENT_USER_ID
            );


            setProjekt(updatedProjekt);
            setDecision("declined");


        } catch (err) {

            console.error(err);

            setError(
                err.message
            );

        }
    };







    if (!projekt) {

        return (
            <>
                <Navbar />

                <div className="container mt-5">
                    Laden...
                </div>

            </>
        );
    }






    return (

        <div>

            <Navbar />


            <div className="container mt-5">


                <div
                    className="card shadow-sm border-0 mx-auto"
                    style={{ maxWidth: "750px" }}
                >


                    <div className="card-body p-5">



                        <div className="text-center mb-4">


                            <h2 className="fw-bold">
                                Projektanfrage
                            </h2>


                            <p className="text-muted">
                                Du wurdest eingeladen, an folgendem Projekt teilzunehmen.
                            </p>


                        </div>




                        <hr />





                        <h3 className="fw-bold">
                            {projekt.titel}
                        </h3>



                        <p className="text-muted">
                            {projekt.beschreibung}
                        </p>






                        <div className="row mt-4">


                            <div className="col-md-6 mb-3">

                                <strong>
                                    Fachbereich
                                </strong>

                                <div>
                                    {projekt.fachbereich}
                                </div>

                            </div>





                            <div className="col-md-6 mb-3">

                                <strong>
                                    Projektart
                                </strong>

                                <div>
                                    {projekt.projektart}
                                </div>

                            </div>





                            <div className="col-md-6 mb-3">

                                <strong>
                                    Semester
                                </strong>

                                <div>
                                    {projekt.semester}
                                </div>

                            </div>





                            <div className="col-md-6 mb-3">

                                <strong>
                                    Teamgröße
                                </strong>

                                <div>
                                    {projekt.gruppenanzahl} Personen
                                </div>

                            </div>



                        </div>







                        {projekt.status === "OFFEN" && !decision && (

                            <>

                                <hr />


                                <p className="text-center fw-semibold">
                                    Möchtest du dieses Projekt annehmen?
                                </p>



                                <div className="d-flex justify-content-center gap-3">


                                    <button
                                        className="btn btn-success px-4"
                                        onClick={handleAccept}
                                    >
                                        Projekt annehmen
                                    </button>




                                    <button
                                        className="btn btn-outline-danger px-4"
                                        onClick={handleDecline}
                                    >
                                        Projekt ablehnen
                                    </button>



                                </div>


                            </>

                        )}







                        {decision === "accepted" && (

                            <div className="alert alert-success mt-4 text-center">

                                Du hast das Projekt angenommen.

                            </div>

                        )}







                        {decision === "declined" && (

                            <div className="alert alert-warning mt-4 text-center">

                                Du hast das Projekt abgelehnt.

                            </div>

                        )}







                        {projekt.status === "ANGENOMMEN" && decision !== "accepted" && (

                            <div className="alert alert-info mt-4 text-center">

                                Dieses Projekt wurde bereits angenommen.

                            </div>

                        )}







                        {projekt.status === "ABGELEHNT" && decision !== "declined" && (

                            <div className="alert alert-warning mt-4 text-center">

                                Dieses Projekt wurde bereits abgelehnt.

                            </div>

                        )}







                        {error && (

                            <div className="alert alert-danger mt-4 text-center">

                                {error}

                            </div>

                        )}



                    </div>


                </div>


            </div>


        </div>

    );

}


export default ProjectInvitation;