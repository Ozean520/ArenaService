const boardElement = document.getElementById("board");
const gameInfo = document.getElementById("gameInfo");

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
let syncPending = false;

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
                board[5][col] !== "EMPTY";

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
            const myDisc = message.yourDisc;
            const currentTurn = message.currentTurn;

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

            if (verdict === "OLD") break;
            if (verdict === "GAP") break;

            lastVersion = message.version;

            board[row][col] = disc;

            currentTurn = message.nextTurn;

            gameStatus = message.status;

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
            for (let row = 0; row < 6; row++)
                for (let col = 0; col < 7; col++)
                    board[row][col] = message.cells[row][col];

            currentTurn = message.currentTurn;
            gameStatus = message.status;

            if (gameStatus === "FINISHED")
                gameInfo.textContent = message.winner === "EMPTY" ?
                    "游戏已结束，平局！" : `游戏已结束，赢家为 ${message.winner}`;
            else gameInfo.textContent = `当前轮到 ${message.currentTurn}！`;

            renderBoard();
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
