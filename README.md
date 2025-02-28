# BlackBox 🔒

A secure desktop application with GTK-based GUI for creating encrypted file containers to protect your sensitive data using AES-256 encryption.

![Java](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)
![GUI](https://img.shields.io/badge/Interface-GTK--Styled-green?logo=gtk)

## Features ✨
- **Military-Grade Encryption**: AES-256-CBC with PBKDF2 key derivation.
- **Secure Storage**: Files are encrypted with unique salts and initialization vectors (IVs).
- **Intuitive CLI Menu**: Easily create, manage, and extract files from containers.
- **Cross-Platform**: Runs anywhere Java is supported (requires GUI environment for file dialogs).
- **Batch Operations**: Add or extract multiple files at once.

## Installation 🛠️
1. **Requirements**:
   - Java 17 or later
   - GUI environment (GTK look-and-feel preferred)

2. **Command-line setup**:
   ```bash
   # Navigate to project root directory
   cd path/to/blackbox-project

   # Compile from source
   javac src/main/java/*.java -d out/

   # Run the application
   java -cp out/ BlackBox
   ```

3. **IntelliJ IDEA (Recommended for Development)**:
   1. Open IntelliJ and select "Open Project"
   2. Navigate to the `src/main/java` directory
   3. Configure SDK:
      - File > Project Structure > Project SDK: Java 17+
   4. Create run configuration:
      - Main class: `BlackBox`
      - Working directory: Project root
   5. Build and run using green arrow (▶️) in toolbar

## Screenshots
![exported_image (3)](https://github.com/user-attachments/assets/0e62df3f-b9d1-499d-9a76-9c933064dfcd)
![exported_image](https://github.com/user-attachments/assets/8182f684-461f-4b52-9c50-a4a7485026ab)
![exported_image (1)](https://github.com/user-attachments/assets/7c721e68-f32f-406e-879f-9b6b6f191094)
![exported_image (2)](https://github.com/user-attachments/assets/531c2fb2-f198-4909-a35c-24b9aabd1d94)



## Usage 📖

### 1. Create a New Container
1. Click "Create New Container" in the main window.
2. Choose a save location via the file dialog.
3. Enter a strong password when prompted.
4. Your encrypted container file (e.g., `my_vault.dat`) will be created.

### 2. Add Files to Container
1. Open your container using "Open Existing Container".
2. Click "Add Files" and select files through the multi-file dialog.
3. Files are encrypted and stored immediately upon selection.

### 3. Extract Files
1. Click "Extract File" in the container management window.
2. Select a file from the displayed list.
3. Choose a save location through the file dialog.

### 4. Save and Exit
Use "Save and Close" to securely write changes and exit the container session.

## Technical Details 🔍
- **Encryption**: AES-256 in CBC mode with PKCS5 padding.
- **Key Derivation**: PBKDF2WithHmacSHA256 (65,536 iterations).
- **Salting**: 16-byte random salt per container.
- **IV**: 16-byte random initialization vector per session.

## Security Notes ⚠️
- 🔑 **Password Strength**: Your password is the only key to decrypt files. Loss = irreversible data loss!
- 🔒 **Container Integrity**: Tampering with the container file will corrupt all data.
- 🛡️ **Best Practices**: Use this for personal files only. Avoid storing highly sensitive data without additional safeguards.

## Development Notes 💻
- **Project Structure**:
  ```
  blackbox-project/
  └── src/
      └── main/
          └── java/
              ├── BlackBox.java
              ├── ContainerManager.java
              ├── EncryptionUtil.java
              └── StoredFile.java
  ```
- **Recommended IDE**: IntelliJ IDEA with built-in Java support
- **Dependencies**: No external libraries required (pure Java implementation)
- **Testing**: Run directly from IDE for debug console access

## Limitations ⚠️
- Requires proper source directory setup for command-line compilation
- IntelliJ automatically handles classpath - manual setup needed for CLI execution
- Requires a GUI environment (GTK look and feel preferred for best experience).
- Maximum file size limited by available memory.
- Not designed for concurrent access.
- Fallback to system theme if GTK is unavailable.

## License 📜
Distributed under the MIT License. See `LICENSE` file for details.

---

💻 **Contribute**: Issues and PRs welcome!  
🌟 **Star this project** if it helps you keep data safe!
