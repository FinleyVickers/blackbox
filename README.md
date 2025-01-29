# BlackBox 🔒

A secure command-line application for creating encrypted file containers to protect your sensitive data using AES-256 encryption.

![Java](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)

## Features ✨
- **Military-Grade Encryption**: AES-256-CBC with PBKDF2 key derivation.
- **Secure Storage**: Files are encrypted with unique salts and initialization vectors (IVs).
- **Intuitive CLI Menu**: Easily create, manage, and extract files from containers.
- **Cross-Platform**: Runs anywhere Java is supported (requires GUI environment for file dialogs).
- **Batch Operations**: Add or extract multiple files at once.

## Installation 🛠️
1. **Requirement**: Java 17 or later.
2. **Compile the Project**:
   ```bash
   javac *.java
   ```
3. **Run the Program**:
   ```bash
   java BlackBox
   ```

## Usage 📖

### 1. Create a New Container
1. Select `1. Create new container` from the main menu.
2. Choose a save location and set a strong password.
3. Your encrypted container file (e.g., `my_vault.dat`) will be created.

### 2. Add Files to Container
1. Open your container using `2. Open existing container`.
2. Choose `1. Add files` and select files via the file dialog.
3. Files are encrypted and stored immediately.

### 3. Extract Files
1. In the container menu, select `3. Extract file`.
2. Choose a file from the list and specify a save location.

### 4. Save and Exit
Use `4. Save and close` to securely write changes to the container.

## Technical Details 🔍
- **Encryption**: AES-256 in CBC mode with PKCS5 padding.
- **Key Derivation**: PBKDF2WithHmacSHA256 (65,536 iterations).
- **Salting**: 16-byte random salt per container.
- **IV**: 16-byte random initialization vector per session.

## Security Notes ⚠️
- 🔑 **Password Strength**: Your password is the only key to decrypt files. Loss = irreversible data loss!
- 🔒 **Container Integrity**: Tampering with the container file will corrupt all data.
- 🛡️ **Best Practices**: Use this for personal files only. Avoid storing highly sensitive data without additional safeguards.

## Limitations ⚠️
- Requires a GUI environment for file selection dialogs.
- Maximum file size limited by available memory.
- Not designed for concurrent access.

## License 📜
Distributed under the MIT License. See `LICENSE` file for details.

---

💻 **Contribute**: Issues and PRs welcome!  
🌟 **Star this project** if it helps you keep data safe!
