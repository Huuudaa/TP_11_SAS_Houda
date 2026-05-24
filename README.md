# TP 11 — Localisation d’un smartphone et envoi des coordonnées vers un serveur distant

Ce projet implémente un système complet de géolocalisation mobile connecté à un serveur distant. Il se compose d’un backend en PHP avec base de données MySQL et d’une application Android développée en Java utilisant la bibliothèque Volley pour l'envoi HTTP.

---
## Demonstration

https://youtube.com/shorts/E_dCOADjIR0?feature=share

##  Partie 1 : Configuration du Serveur (PHP & MySQL)

Le serveur reçoit les requêtes HTTP POST envoyées par le smartphone, crée un objet métier `Position`, et l'enregistre en base de données.

### 1. Base de données MySQL
1. Lancez votre serveur de base de données (ex: **XAMPP**, **WAMP**, ou **MAMP**).
2. Ouvrez **phpMyAdmin** et créez une base de données nommée `localisation`.
3. Importez ou exécutez le script SQL présent dans [localisation/localisation.sql](file:///c:/Users/HP%20PRO/AndroidStudioProjects/TP_11_SAS_Houda/localisation/localisation.sql) :
   ```sql
   CREATE DATABASE IF NOT EXISTS localisation
   CHARACTER SET utf8mb4
   COLLATE utf8mb4_unicode_ci;

   USE localisation;

   CREATE TABLE IF NOT EXISTS position (
       id INT AUTO_INCREMENT PRIMARY KEY,
       latitude DOUBLE NOT NULL,
       longitude DOUBLE NOT NULL,
       date_position DATETIME NOT NULL,
       imei VARCHAR(50) NOT NULL
   ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
   ```

### 2. Déploiement des scripts PHP
1. Copiez le dossier `localisation` (qui se trouve à la racine de ce projet) dans le répertoire public de votre serveur web (ex: `C:/xampp/htdocs/` pour XAMPP ou `C:/wamp64/www/` pour WAMP).
2. La structure de fichiers du serveur doit ressembler à ceci :
   ```text
   localisation/
   ├── classe/
   │   └── Position.php         # Classe métier Position (Encapsulation)
   ├── connexion/
   │   └── Connexion.php       # Connexion PDO centralisée
   ├── dao/
   │   └── IDao.php            # Interface générique d'accès aux données (CRUD)
   ├── service/
   │   └── PositionService.php # Implémentation DAO pour l'insertion SQL
   ├── createPosition.php      # Point d'entrée HTTP (reçoit les requêtes POST)
   └── localisation.sql        # Script d'initialisation de la base de données
   ```
3. Si votre base de données MySQL utilise un utilisateur ou un mot de passe différent de `root` et `""` (vide), modifiez les identifiants dans le fichier [localisation/connexion/Connexion.php](file:///c:/Users/HP%20PRO/AndroidStudioProjects/TP_11_SAS_Houda/localisation/connexion/Connexion.php) :
   ```php
   $login = 'votre_utilisateur';
   $password = 'votre_mot_de_passe';
   ```

### 3. Test de validation du serveur
Avant de lancer l'application Android, vous pouvez valider le bon fonctionnement de l'API avec une commande `curl` dans votre terminal ou via un outil comme **Postman** :
```bash
curl -X POST -d "latitude=31.6295&longitude=-7.9811&date_position=2026-05-24 12:00:00&imei=123456789012345" http://localhost/localisation/createPosition.php
```
Le serveur doit répondre : **`Position enregistree avec succes`**.

---

##  Partie 2 : Configuration de l'Application Android

L'application Android écoute en continu le capteur GPS du smartphone et transmet les coordonnées au serveur dès qu'elle détecte un déplacement significatif.

### 1. Permissions requises
L'application requiert les permissions déclarées dans [AndroidManifest.xml](file:///c:/Users/HP%20PRO/AndroidStudioProjects/TP_11_SAS_Houda/app/src/main/AndroidManifest.xml) :
* `ACCESS_FINE_LOCATION` : Localisation précise via puce GPS.
* `ACCESS_COARSE_LOCATION` : Localisation approximative via Wi-Fi/Réseau.
* `INTERNET` : Communication avec le serveur web distant.
* `READ_PHONE_STATE` : Récupération de l'identifiant IMEI du téléphone (pour le TP).

*Note sur la sécurité : Pour tester le serveur PHP sur un réseau local en HTTP classique (non sécurisé par SSL/HTTPS), la configuration `android:usesCleartextTraffic="true"` a été ajoutée dans la balise `<application>` du manifeste.*

### 2. Bibliothèque de réseau Volley
La dépendance Volley est ajoutée dans [app/build.gradle.kts](file:///c:/Users/HP%20PRO/AndroidStudioProjects/TP_11_SAS_Houda/app/build.gradle.kts) :
```kotlin
implementation("com.android.volley:volley:1.2.1")
```

### 3. Configuration de l'URL du serveur
Ouvrez le fichier [MainActivity.java](file:///c:/Users/HP%20PRO/AndroidStudioProjects/TP_11_SAS_Houda/app/src/main/java/com/example/tp_11_sas_houda/MainActivity.java) et adaptez la variable `insertUrl` selon votre environnement de test :
* **Avec l'Émulateur Android Studio** : Gardez l'adresse par défaut `"http://10.0.2.2/localisation/createPosition.php"`. L'adresse IP `10.0.2.2` est une passerelle spéciale permettant d'accéder au `localhost` de l'ordinateur hôte.
* **Avec un Smartphone Physique** : Remplacez `10.0.2.2` par l'adresse IP locale de votre ordinateur (ex: `"http://192.168.1.50/localisation/createPosition.php"`). **Important** : Le téléphone et l'ordinateur doivent être connectés au même réseau Wi-Fi.

### 4. Gestion robuste de l'identifiant IMEI
Sur les versions modernes d'Android (Android 10 / API 29 et ultérieures), l'accès à l'IMEI via `telephonyManager.getDeviceId()` est restreint pour des raisons de confidentialité et lève une exception `SecurityException`.
Pour pallier ce problème, le code intègre un mécanisme de repli automatique sur le **`ANDROID_ID`** (`Settings.Secure.ANDROID_ID`) sur les appareils récents, assurant ainsi la compatibilité et la stabilité de l'application sur tous les smartphones.

---

## Procédure de Test de l'application

1. **Lancez le serveur local** (ex: Apache et MySQL sur XAMPP).
2. **Ouvrez le projet sous Android Studio**.
3. **Compilez et exécutez** l'application sur un émulateur ou sur votre smartphone physique.
4. **Acceptez les permissions** de localisation et d'état du téléphone à l'invite.
5. **Simulez des déplacements** :
   * *Sur Émulateur* : Cliquez sur les trois petits points `...` dans la barre latérale de l'émulateur, allez dans l'onglet **Location**, puis modifiez les coordonnées (Latitude / Longitude) et cliquez sur **Send**.
   * *Sur Appareil Physique* : Déplacez-vous de plus de 150 mètres pour déclencher la mise à jour automatique.
   * *Forcer l'envoi* : Utilisez le bouton **"Forcer l'envoi de la position"** pour transmettre immédiatement les coordonnées affichées à l'écran vers le serveur PHP.
6. **Vérifiez en Base de Données** :
   Consultez la table `position` sous phpMyAdmin. Chaque envoi génère une nouvelle ligne enregistrant la latitude, la longitude, l'heure exacte et l'identifiant de l'appareil (IMEI ou ANDROID_ID).

---

## Bonnes Pratiques Rappelées
* Assurez-vous que le GPS de l'appareil mobile est activé dans les réglages système.
* Si le serveur distant ne répond pas, désactivez temporairement le pare-feu de votre ordinateur (ou configurez une règle d'accès pour le port 80).
* Utilisez toujours des requêtes préparées côté PHP pour sécuriser les insertions contre les injections SQL (ce qui est fait dans `PositionService`).
