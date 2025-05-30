# Camera App

## Overview
The Camera App is a Java-based GUI application designed to provide users with a seamless experience for capturing photos and videos, editing media, and organizing them. The app supports features such as timed and delayed captures, media formatting, and a gallery view for browsing saved items.

## Features
- Capture photos and videos with customizable settings.
- Edit media with filters and formatting options.
- Organize media in a gallery grouped by date.
- Scrollable gallery with previews for photos and videos.
- Supports OpenCV for image and video processing.

## Requirements
- Java Development Kit (JDK) 11 or higher.
- IntelliJ IDEA 2025.1 or any compatible IDE.
- OpenCV library (version 4.10.0 or higher). Download from the [OpenCV website](https://opencv.org/releases.html).
## Installation

### Step 1: Clone the Repository

```bash
git clone https://github.com/Joel-Fah/camera-app.git
cd camera-app
```

### Step 2: Download OpenCV
1. Visit the [OpenCV website](https://opencv.org/release.html) and download the latest version of OpenCV.
   - Download the [Windows .exe installer](https://github.com/opencv/opencv/releases/download/4.11.0/opencv-4.11.0-windows.exe) if you are using Windows.
2. Extract the downloaded archive and locate the following files under the `build/java` directory:
   - `opencv-<version>.jar` (Java bindings for OpenCV).
   - `opencv-<version>.dll` (native library for OpenCV).

### Step 3: Configure OpenCV in Your Project
1. Copy the `opencv-<version>.jar` file to the `lib` folder in the project directory.
2. Add the `.jar` file to your project's classpath:
   - In IntelliJ IDEA, go to `File > Project Structure > Libraries`.
   - Click **Add** and select the `.jar` file.
   - In the same way, add the `opencv-<version>.dll` under `java\build\x64` file to the project:
     - Click **Add** and select the `.dll` file.

### Step 4: Build the Project
1. Open the project in IntelliJ IDEA.
2. Ensure the `src` folder is marked as a source root.
3. Build the project using **Build > Build Project**.

## Launching the Application
1. Run the `Main` class located in `src/app/Main.java`.
2. The application window will open, allowing you to use the camera features.

## External Libraries
- **OpenCV**: Used for image and video processing. Ensure both the `.jar` and `.dll` files are properly configured.

## Notes
- The gallery view requires access to the `Pictures/CameraApp` directory in your home folder.
- Ensure your system has a working camera for capturing photos and videos.

## License
This project is licensed under the MIT License. See the `LICENSE` file for details.
```