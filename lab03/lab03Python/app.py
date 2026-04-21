from flask import Flask, render_template, jsonify, request
import copy
import random

app = Flask(__name__)

class Card:
    def __init__(self, rank, suit):
        self.rank, self.suit = rank, suit
    def __str__(self):
        return f"{self.rank}{self.suit}"

class Deck:
    def __init__(self):
        suits = ['♠', '♥', '♦', '♣']
        ranks = ['2', '3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K', 'A']
        self.cards = [Card(r, s) for s in suits for r in ranks]
        random.shuffle(self.cards)
    def deal(self, num):
        dealt, self.cards = self.cards[:num], self.cards[num:]
        return dealt

class GameMemento:
    def __init__(self, state):
        self._state = copy.deepcopy(state)
    def get_state(self):
        return self._state

class Caretaker:
    def __init__(self):
        self._history = []
        self._current_index = -1
        
    def save(self, memento):
        if self._current_index < len(self._history) - 1:
            self._history = self._history[:self._current_index + 1]
        
        self._history.append(memento)
        self._current_index = len(self._history) - 1
        print(f"Сохранено. Текущий индекс: {self._current_index}")

    def undo(self):
        if self._current_index >= 0:
            print(f"Откат к индексу: {self._current_index}")
            result = self._history[self._current_index]
            self._current_index -= 1
            return result
        return None

class PokerGame:
    def __init__(self):
        self.small_blind = 10
        self.big_blind = 20
        
        self.caretaker = Caretaker()
        self.start_new_hand()
        

    def start_new_hand(self, p_chips=None, b_chips=None):
        new_p_chips = 1000
        new_b_chips = 1000

        if hasattr(self, 'state'):
            new_p_chips = self.state.get('player_chips', 1000)
            new_b_chips = self.state.get('bot_chips', 1000)

        if p_chips is not None:
            new_p_chips = p_chips
        if b_chips is not None:
            new_b_chips = b_chips

        if new_p_chips < self.small_blind or new_b_chips < self.small_blind:
            print("Баланс исчерпан! Перезапуск игры...")
            new_p_chips = 1000
            new_b_chips = 1000

        new_p_chips -= self.small_blind
        new_b_chips -= self.big_blind

        deck = Deck()
        self.state = {
            'player_chips': new_p_chips,
            'bot_chips': new_b_chips,
            'pot': self.small_blind + self.big_blind,
            'stage': 'Префлоп',
            'deck': deck,
            'player_hand': deck.deal(2),
            'bot_hand': deck.deal(2),
            'community_cards': []
        }
           
    def manual_save(self):
        memento = GameMemento(self.state)
        self.caretaker.save(memento)
        print("Состояние сохранено вручную!")

    def restore_state(self):
        memento = self.caretaker.undo()
        if memento:
            self.state = memento.get_state()
    
    def _next_stage(self):
        stage = self.state['stage']
        deck = self.state['deck']
        
        if stage == 'Префлоп':
            self.state['community_cards'].extend(deck.deal(3))
            self.state['stage'] = 'Флоп'
        elif stage == 'Флоп':
            self.state['community_cards'].extend(deck.deal(1))
            self.state['stage'] = 'Терн'
        elif stage == 'Терн':
            self.state['community_cards'].extend(deck.deal(1))
            self.state['stage'] = 'Ривер'
        elif stage == 'Ривер':
            self.state['stage'] = 'Шоудаун'
            self.resolve_winner()
            
    def place_bet(self, amount):
        if self.state['stage'] == 'Шоудаун':
            return

        if self.state['player_chips'] >= amount:            
            self.state['player_chips'] -= amount
            self.state['bot_chips'] -= amount
            self.state['pot'] += (amount * 2)
            
            self._next_stage() 
            print(f"Стадия изменена на: {self.state['stage']}")
    
    def resolve_winner(self):
        p_score, _ = self.get_hand_rank(self.state['player_hand'] + self.state['community_cards'])
        b_score, _ = self.get_hand_rank(self.state['bot_hand'] + self.state['community_cards'])
        
        pot = self.state['pot']
        
        if p_score > b_score:
            self.state['player_chips'] += pot
        elif b_score > p_score:
            self.state['bot_chips'] += pot
        else:
            # Ничья — делим банк
            self.state['player_chips'] += pot // 2
            self.state['bot_chips'] += pot // 2
            
        self.state['pot'] = 0 # Обнуляем банк для следующей раздачи

    def get_hand_rank(self, cards):
            ranks = '2345678910JQKA'
            # Преобразуем карты в числовые значения индексов
            values = sorted([ranks.index(str(c.rank)) for c in cards], reverse=True)
            suits = [c.suit for c in cards]
            
            # Проверка на Флеш 
            suit_counts = {s: suits.count(s) for s in set(suits)}
            flush_suit = next((s for s, count in suit_counts.items() if count >= 5), None)
            is_flush = False
            
            # Проверка на Стрит 
            unique_values = sorted(list(set(values)), reverse=True)
            is_straight = False
            straight_high_card = -1
            
            if len(unique_values) >= 5:
                for i in range(len(unique_values) - 4):
                    if unique_values[i] - unique_values[i+4] == 4:
                        is_straight = True
                        straight_high_card = unique_values[i]
                        break
                # Специфический случай: Стрит от Туза до пятерки (A-2-3-4-5)
                if not is_straight and set([12, 0, 1, 2, 3]).issubset(set(unique_values)):
                    is_straight = True
                    straight_high_card = 3

            # Подсчет совпадений 
            counts = {v: values.count(v) for v in set(values)}
            count_values = list(counts.values())
                        
            if is_flush:
                # Оставляем только карты той масти, которой у нас Флеш
                flush_cards_values = sorted([ranks.index(str(c.rank)) for c in cards if c.suit == flush_suit], reverse=True)
                # Проверяем, есть ли внутри этих карт Стрит
                for i in range(len(flush_cards_values) - 4):
                    if flush_cards_values[i] - flush_cards_values[i+4] == 4:
                        # Если старшая карта Стрит-флеша — Туз
                        if flush_cards_values[i] == 12:
                            return (9, "Рояль-флеш")
                        return (8, f"Стрит-флеш ({ranks[flush_cards_values[i]]} хай)")
                
            if 4 in count_values: 
                return (7, "Каре")
                
            if 3 in count_values and 2 in count_values: 
                return (6, "Фулл-хаус")
                
            if is_flush: 
                return (5, "Флеш")
                
            if is_straight: 
                return (4, f"Стрит ({ranks[straight_high_card]} хай)")
                
            if 3 in count_values: 
                return (3, "Сет (Тройка)")
                
            if count_values.count(2) >= 2: 
                return (2, "Две пары")
                
            if 2 in count_values: 
                return (1, "Пара")
                
            return (0, f"Старшая карта {ranks[values[0]]}")
        
    def get_json_state(self):
        def card_to_filename(card):
            suit_map = {'♠': 'spades', '♥': 'hearts', '♦': 'diamonds', '♣': 'clubs'}
            rank_map = {'J': 'jack', 'Q': 'queen', 'K': 'king', 'A': 'ace'}
            rank = rank_map.get(str(card.rank), str(card.rank))
            return f"{rank}_of_{suit_map[card.suit]}.png"

        all_cards = self.state['player_hand'] + self.state['community_cards']
        _, combo_name = self.get_hand_rank(all_cards) if all_cards else (0, "Раздача")

        return {
            'player_chips': self.state['player_chips'],
            'bot_chips': self.state['bot_chips'],
            'pot': self.state['pot'],
            'stage': self.state['stage'],
            'player_hand': [card_to_filename(c) for c in self.state['player_hand']],
            'bot_hand': [card_to_filename(c) for c in self.state['bot_hand']] if self.state['stage'] == 'Шоудаун' else ['back.png', 'back.png'],
            'community_cards': [card_to_filename(c) for c in self.state['community_cards']],
            'player_combo': combo_name,
            'status': self.get_status_text()
        }
        

    def determine_winner(self):
        p_score, p_name = self.get_hand_rank(self.state['player_hand'] + self.state['community_cards'])
        b_score, b_name = self.get_hand_rank(self.state['bot_hand'] + self.state['community_cards'])
        
        if p_score > b_score:
            return 'player', p_name, b_name
        elif b_score > p_score:
            return 'bot', p_name, b_name
        else:
            return 'draw', p_name, b_name

    def get_status_text(self):
        stage = self.state['stage']
        
        if stage == 'Шоудаун':
            winner, p_name, b_name = self.determine_winner()
            if winner == 'player':
                return f"Вы выиграли! У вас {p_name}, а у бота {b_name}."
            elif winner == 'bot':
                return f"Бот выиграл! У него {b_name}, а у вас {p_name}."
            else:
                return f"Ничья! Оба собрали {p_name}."
                
        return f"Стадия: {stage}. Ждем ваших действий."
            
game = PokerGame()

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/api/state')
def get_state():
    return jsonify(game.get_json_state())

@app.route('/api/bet', methods=['GET', 'POST'])
def make_bet():
    if request.method == 'POST':
        data = request.get_json()
        amount = data.get('amount', 50)
    else:
        amount = 50

    game.place_bet(int(amount))
    return jsonify(game.get_json_state())

@app.route('/api/undo')
def undo_move():
    game.restore_state()
    return jsonify(game.get_json_state())

@app.route('/api/new_hand')
def new_hand():
    p_chips = request.args.get('p_chips', type=int)
    b_chips = request.args.get('b_chips', type=int)
    
    game.start_new_hand(p_chips, b_chips)
    return jsonify(game.get_json_state())

@app.route('/api/save')
def save_game():
    game.manual_save()
    return jsonify({"status": "success", "message": "Игра сохранена"})

if __name__ == '__main__':
    app.run(debug=True)