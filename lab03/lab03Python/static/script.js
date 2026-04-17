// Функция обновления всего интерфейса данными от Flask
function renderGame(state) {
    document.getElementById('bot-chips').innerText = state.bot_chips;
    document.getElementById('player-chips').innerText = state.player_chips;
    document.getElementById('pot').innerText = state.pot;
    document.getElementById('stage-name').innerText = `--- ${state.stage} ---`;

    // Отрисовка карт через вспомогательную функцию
    displayCards('bot-cards', state.bot_hand);
    displayCards('player-cards', state.player_hand);
    displayCards('community-cards', state.community_cards);

    localStorage.setItem('poker_player_chips', state.player_chips);
    localStorage.setItem('poker_bot_chips', state.bot_chips);

    const range = document.getElementById('bet-range');
    const betValueDisplay = document.getElementById('bet-value');
// 1. Ограничиваем максимум ползунка текущим балансом игрока
    range.max = state.player_chips;

    // 2. Если после Undo или новой раздачи ставка больше баланса, сбрасываем её
    if (parseInt(range.value) > state.player_chips) {
        range.value = Math.min(50, state.player_chips);
        betValueDisplay.innerText = range.value;
    }

    const comboElem = document.getElementById('player-combo');
        if (comboElem && state.player_combo) {
            comboElem.innerText = "Ваша комбинация: " + state.player_combo;
        }
    const statusElem = document.getElementById('game-status');
    if (state.stage === 'Шоудаун') {
        statusElem.innerText = state.status; // Например: "Бот выиграл! У него Флеш"
        statusElem.style.color = "#ffeb3b"; // Подсветим золотым
    } else {
        statusElem.innerText = "";
    }
    // Обновляем общие карты на столе
    displayCards('community-cards', state.community_cards);
    
    // Выведи в консоль для отладки, чтобы видеть, что прислал сервер
    console.log("Карты на столе:", state.community_cards);
    }

// localStorage.setItem('player_chips', state.player_chips);
// localStorage.setItem('bot_chips', state.bot_chips);

function displayCards(elementId, cardsArray) {
    const container = document.getElementById(elementId);
    if (!container) return; // Проверка, что блок вообще есть
    
    container.innerHTML = ''; // Очищаем старые карты

    cardsArray.forEach(cardFileName => {
        const img = document.createElement('img');
        
        // ВНИМАНИЕ: Проверь эту строку. 
        // Если в Python ты уже добавил ".png", здесь его быть НЕ должно.
        img.src = "/static/images/" + cardFileName; 
        
        img.className = 'card-img'; // Твой стиль из CSS
        container.appendChild(img);
    });
}
async function sendAction(endpoint) {
    let options = { method: 'GET' };

    if (endpoint === 'bet') {
        const amount = document.getElementById('bet-range').value;
        options = {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ amount: parseInt(amount) })
        };
    }

    try {
        const response = await fetch(`/api/${endpoint}`, options);
        const newState = await response.json();
        renderGame(newState);
    } catch (err) {
        console.error("Ошибка запроса:", err);
    }
}

async function saveCurrentGame() {
    const response = await fetch('/api/save');
    const result = await response.json();
    if (result.status === 'success') {
        alert("Точка сохранения создана!"); 
    }
}

document.addEventListener('DOMContentLoaded', () => {
    // Привязка кнопок
    document.getElementById('btn-bet').onclick = () => sendAction('bet');
    document.getElementById('btn-undo').onclick = () => sendAction('undo');
    document.getElementById('btn-save').onclick = saveCurrentGame;

    // Новая раздача с учетом localStorage
    document.getElementById('btn-new').onclick = () => {
        const p = localStorage.getItem('poker_player_chips');
        const b = localStorage.getItem('poker_bot_chips');
        
        if (p && b) {
            fetch(`/api/new_hand?p_chips=${p}&b_chips=${b}`)
                .then(res => res.json())
                .then(state => renderGame(state));
        } else {
            sendAction('new_hand');
        }
    };

    // Ползунок ставки
    const betRange = document.getElementById('bet-range');
    const betValue = document.getElementById('bet-value');
    if (betRange) {
        betRange.oninput = function() {
            betValue.innerText = this.value;
        };
    }

    // Загрузка начального состояния
    sendAction('state');
});

// 1. Функция сохранения (вынесена вверх для удобства)
async function saveCurrentGame() {
    const response = await fetch('/api/save');
    const result = await response.json();
    if (result.status === 'success') {
        alert("Игра успешно сохранена!"); 
    }
}

// 2. Единый блок инициализации при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    // Объявляем ВСЕ кнопки внутри
    const btnBet = document.getElementById('btn-bet');
    const btnUndo = document.getElementById('btn-undo');
    const btnNew = document.getElementById('btn-new');
    const btnSave = document.getElementById('btn-save'); // Добавили кнопку сохранения
    
    const betRange = document.getElementById('bet-range');
    const betValue = document.getElementById('bet-value');

    // Привязываем логику ползунка
    if (betRange && betValue) {
        betRange.oninput = function() {
            betValue.innerText = this.value;
        };
    }

    // Привязываем клики (с проверкой на существование кнопок)
    if (btnBet) btnBet.onclick = () => sendAction('bet');
    if (btnUndo) btnUndo.onclick = () => sendAction('undo');
    if (btnSave) btnSave.onclick = saveCurrentGame; // Привязываем сохранение ТУТ

    // Логика кнопки "Новая раздача"
    if (btnNew) {
        btnNew.onclick = () => {
            const p = localStorage.getItem('poker_player_chips');
            const b = localStorage.getItem('poker_bot_chips');
            
            if (p && b) {
                fetch(`/api/new_hand?p_chips=${p}&b_chips=${b}`)
                    .then(res => res.json())
                    .then(state => renderGame(state));
            } else {
                sendAction('new_hand');
            }
        };
    }

    // Начальная загрузка данных
    const savedPlayer = localStorage.getItem('poker_player_chips');
    const savedBot = localStorage.getItem('poker_bot_chips');

    if (savedPlayer && savedBot) {
        fetch(`/api/new_hand?p_chips=${savedPlayer}&b_chips=${savedBot}`)
            .then(res => res.json())
            .then(state => renderGame(state));
    } else {
        sendAction('new_hand'); 
    }
});