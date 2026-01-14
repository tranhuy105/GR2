# Client (Frontend)

Frontend app này build bằng **Next.js 16**, dùng để visualize map, quản lý đơn hàng và tương tác với system.

## Stack/Tools

*   **Framework**: Next.js 16 (App Router), React 19.
*   **Styling**: TailwindCSS v4.
*   **Map**: Leaflet + React-Leaflet.
*   **State**: Zustands.
*   **UI Libs**: Radix UI, Lucide React.
*   **Package Manager**: npm/yarn/pnpm.

## Setup & Run

1.  Vào folder client:
    ```bash
    cd client
    ```

2.  Cài dependencies:
    ```bash
    npm install
    ```

3.  Chạy dev server:
    ```bash
    npm run dev
    ```

App sẽ chạy ở: [http://localhost:3000](http://localhost:3000)

## Project Structure

*   `src/app`: Main app logic (Next.js App Router).
*   `src/components`: Reusable components (Map, UI blocks, Layouts).
*   `src/lib`: Utils, hooks, configs.
*   `src/store`: Global state (Zustand stores).
