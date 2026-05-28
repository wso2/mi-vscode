# Contributing to MI for VS Code

Thank you for your interest in contributing. This repository is focused on Micro Integrator for VS Code and its supporting local packages.

---

## How to Contribute

1. **Fork the Repository**  
   Click the "Fork" button on GitHub and clone your fork locally.

2. **Create a Branch**  
   Create a new branch for your feature, bugfix, or improvement:
   ```bash
   git checkout -b my-feature-branch
   ```

3. **Initialize Submodules**  
   This repository uses the `vscode-extensions` git submodule for shared packages:
   ```bash
   pnpm run init-submodules
   ```

4. **Install Dependencies**  
   Use [Rush](https://rushjs.io/) for dependency management:
   ```bash
   rush install
   ```

5. **Set Up Environment Variables**  
   `mi-extension` requires a `.env` file:
   ```bash
   cp packages/mi-extension/.env.example packages/mi-extension/.env
   ```
   Fill in the required values before running the extension locally.

6. **Make Your Changes**  
   Follow the code style and structure of the project. Add tests if applicable.

7. **Build and Test**  
   Build the language server or the full MI extension using Rush:
   ```bash
   rush build --to language-server
   rush build --to micro-integrator
   ```
   Run package-specific tests when your changes affect those areas.

8. **Commit and Push**  
   Commit your changes with a clear message and push your branch to your fork.

9. **Open a Pull Request**  
   Go to the main repository and open a pull request from your branch. Describe your changes and reference any related issues.

---

## Guidelines

- **Follow the directory structure** as described in `SOURCE_ORG.md`.
- **Keep local MI source under `packages/`** and do not reintroduce the old `workspaces/mi/...` structure.
- **Use shared packages from the `submodules/vscode-extensions` git submodule** instead of copying them into this repository.
- **Use Rush commands** for all dependency and build operations.
- **Preserve the language server flow** so `packages/mi-language-server` builds before artifacts are copied into `packages/mi-extension/ls`.
- **Write clear commit messages** and PR descriptions.
- **Add or update tests** for your changes.
- **Ensure your code passes linting and CI checks.**
- **Do not commit sensitive information** (such as secrets or credentials).

---

## Reporting Issues

- Use the [issue templates](.github/ISSUE_TEMPLATE) for bug reports, feature requests, improvements, questions, or tasks.
- Provide as much detail as possible, including steps to reproduce, environment, and screenshots if applicable.

---

## Community & Support

- For questions, open an issue or visit our [community page](https://wso2.com/community/).
- Please review our [Code of Conduct](https://github.com/wso2/code-of-conduct) before contributing.

---

Thank you for helping us improve MI for VS Code.
