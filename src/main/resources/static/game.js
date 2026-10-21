const boardElement = document.getElementById("board");
const gameInfo = document.getElementById("gameInfo");
const MAX_SYNC_ATTEMPTS = 3;

const protocol = location.protocol === "https:"
    ? "wss:" : "ws:";

const ws = new WebSocket(
    `${protocol}//${location.host}/ws`
);

let roomId = null;
let myDisc = null;
let currentTurn = null;
let gameStatus = "WAITING";
let lastVersion = 0;
let highestSeenVersion = 0;
let syncPending = false;    /* 是否正在等待完整快照，等待时禁止落子 */
let syncAttempts = 0;   /* 连续尝试同步的次数 */
let syncFailed = false; /* 同步是否失败 */

const board = Array.from(
    { length: 6 },
    () => Array(7).fill("EMPTY")
);

function checkVersion(version) {
    if (version <= lastVersion) return "OLD";
    if (version > lastVersion + 1) return "GAP";
    return "NEXT";
}

/* 绘制棋盘 */
function renderBoard() {
    boardElement.replaceChildren();

    for (let row = 5; row >= 0; row--)
        for (let col = 0; col < 7; col++) {
            const cell = document.createElement("button");

            cell.className =
                `cell ${board[row][col].toLowerCase()}`;

            cell.disabled =
                gameStatus !== "PLAYING" ||
                currentTurn !== myDisc ||
                board[5][col] !== "EMPTY" ||
                syncPending ||
                syncFailed;

            cell.onclick = () => {
                ws.send(JSON.stringify({
                    type: "MOVE",
                    roomId: roomId,
                    column: col
                }));
            };

            boardElement.appendChild(cell);
        }
}

/* 请求快照 */
function requestGameSync() {
    if (syncPending ||
        roomId === null ||
        ws.readyState !== WebSocket.OPEN)
        return;

    if (syncAttempts >= MAX_SYNC_ATTEMPTS) {
        gameInfo.textContent = "状态同步连续失败，请重新进入游戏";
        syncFailed = true;
        return;
    }

    syncPending = true;
    syncAttempts++;

    ws.send(JSON.stringify({
        type: "SYNC_GAME",
        roomId: roomId,
    }));

    renderBoard();
}

/* 验证棋盘 */
function validCells(cells) {
    return Array.isArray(cells) &&
        cells.length === 6 &&
        cells.every(row =>
            Array.isArray(row) &&
            row.length === 7 &&
            row.every(cell =>
                ["EMPTY", "RED", "YELLOW"].includes(cell)
            )
        );
}

/* 发送创建房间请求 */
document.getElementById("createButton")
    .onclick = () => {
    ws.send(JSON.stringify({
        type: "CREATE_ROOM"
    }));
};

/* 发送加入房间请求 */
document.getElementById("joinButton")
    .onclick = () => {
    const id = document.getElementById("roomInput").value;

    ws.send(JSON.stringify({
        type: "JOIN_ROOM",
        roomId: id
    }));
};

ws.onmessage = (event) => {
    const message = JSON.parse(event.data);

    switch (message.type) {

        case "WELCOME": {
            console.log(
                "My player ID:", message.playerId
            );
            break;
        }

        case "ROOM_CREATED": {
            roomId = message.roomId;

            gameInfo.textContent = `房间 ${roomId} 已创建，等待玩家加入`;
            break;
        }

        case "GAME_STARTED": {
            roomId = message.roomId;
            myDisc = message.yourDisc;
            currentTurn = message.currentTurn;

            lastVersion = 0;
            highestSeenVersion = 0;
            syncPending = false;
            syncAttempts = 0;

            gameStatus = "PLAYING";

            gameInfo.textContent = `游戏开始！你是 ${myDisc} 棋子！\n
                现在是 ${currentTurn} 的回合！`;

            renderBoard();

            break;
        }

        case "GAME_UPDATE": {
            const row = message.row;
            const col = message.column;
            const disc = message.disc;
            const verdict = checkVersion(message.version);

            if (message.roomId !== roomId ||
                !Number.isInteger(message.version) ||
                message.version < 0 || message.version > 42) {
                const log = document.getElementById("log");
                log.textContent = "请求更新失败！";
                break;
            }

            if (message.version > highestSeenVersion) highestSeenVersion = message.version;

            if (verdict === "OLD") break;

            if (syncPending) break;

            if (verdict === "GAP") {
                requestGameSync();
                break;
            }

            board[row][col] = disc;

            currentTurn = message.nextTurn;
            gameStatus = message.status;

            lastVersion = message.version;
            if (gameStatus === "FINISHED") {
                if (message.winner === "EMPTY")
                    gameInfo.textContent = '游戏结束，平局！';
                 else
                    gameInfo.textContent = '游戏结束，赢家是：' + message.winner;
            } else
                gameInfo.textContent = `现在是 ${currentTurn} 的回合！`;

            renderBoard();
            break;
        }

        case "GAME_SNAPSHOT": {
            if (message.roomId !== roomId) break;

            if (
                !Number.isInteger(message.version) ||
                message.version < 0 ||
                message.version > 42 ||
                !validCells(message.cells) ||
                !["PLAYING", "FINISHED"].includes(message.status) ||
                !["RED", "YELLOW", "EMPTY"].includes(message.currentTurn) ||
                !["RED", "YELLOW", "EMPTY"].includes(message.winner)
            ) break;

            if (message.version >= lastVersion) {
                for (let row = 0; row < 6; row++)
                    for (let col = 0; col < 7; col++)
                        board[row][col] = message.cells[row][col];

                currentTurn = message.currentTurn;
                gameStatus = message.status;

                if (gameStatus === "FINISHED")
                    gameInfo.textContent = message.winner === "EMPTY" ?
                        "游戏已结束，平局！" : `游戏已结束，赢家为 ${message.winner}`;
                else gameInfo.textContent = `当前轮到 ${message.currentTurn}！`;

                lastVersion = message.version;
            }

            syncPending = false;

            if (lastVersion < highestSeenVersion) requestGameSync();
            else {
                highestSeenVersion = lastVersion;
                syncAttempts = 0;
                renderBoard();
            }

            break;
        }

        case "ERROR": {
            const log = document.getElementById("log");

            log.textContent = `[${message.code}] ${message.message}\n`;
            break;
        }
    }
};

ws.onopen = () => {
    document.getElementById("connectionStatus")
        .textContent = "已连接";
};

ws.onclose = () => {
    gameStatus = "DISCONNECTED";
    document.getElementById("connectionStatus")
        .textContent = "连接已断开";
    renderBoard();
};

renderBoard();
