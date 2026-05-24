<?php
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    include_once 'service/PositionService.php';
    create();
} else {
    // Permet un diagnostic rapide si on ouvre la page dans un navigateur
    echo "Veuillez envoyer une requête POST contenant les paramètres latitude, longitude, date_position et imei.";
}

function create() {
    // Vérification des paramètres requis
    if (isset($_POST['latitude']) && isset($_POST['longitude']) && isset($_POST['date_position']) && isset($_POST['imei'])) {
        $latitude = $_POST['latitude'];
        $longitude = $_POST['longitude'];
        $datePosition = $_POST['date_position'];
        $imei = $_POST['imei'];

        $service = new PositionService();
        $position = new Position(null, $latitude, $longitude, $datePosition, $imei);
        
        try {
            $service->create($position);
            echo "Position enregistree avec succes";
        } catch (Exception $e) {
            header("HTTP/1.1 500 Internal Server Error");
            echo "Erreur serveur : " . $e->getMessage();
        }
    } else {
        header("HTTP/1.1 400 Bad Request");
        echo "Erreur : Parametres manquants. Requis : latitude, longitude, date_position, imei.";
    }
}
?>
