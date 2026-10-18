(() => {
    'use strict';

    const $ = id => document.getElementById(id);
    const safeStore = {
        get(key, fallback) {
            try { const value = localStorage.getItem(key); return value == null ? fallback : value; }
            catch (_) { return fallback; }
        },
        set(key, value) { try { localStorage.setItem(key, String(value)); } catch (_) {} }
    };
    const shuffle = items => {
        const copy = [...items];
        for (let i = copy.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [copy[i], copy[j]] = [copy[j], copy[i]];
        }
        return copy;
    };

    /* Shared PawPause timer */
    let pauseInterval = null;
    let pauseRemaining = 0;
    const waitTimer = $('waitTimer');
    const waitTimerLabel = $('waitTimerLabel');
    const stopPause = $('stopPause');
    const pauseButtons = [...document.querySelectorAll('[data-pause-minutes]')];
    const renderPause = () => {
        if (pauseRemaining <= 0) return;
        const mins = Math.floor(pauseRemaining / 60);
        const secs = pauseRemaining % 60;
        waitTimer.textContent = `${mins}:${String(secs).padStart(2, '0')}`;
        waitTimerLabel.textContent = 'little joy time';
    };
    const stopPauseTimer = (completed = false) => {
        if (pauseInterval) clearInterval(pauseInterval);
        pauseInterval = null;
        pauseRemaining = 0;
        pauseButtons.forEach(b => b.classList.remove('active'));
        stopPause.disabled = true;
        waitTimer.textContent = completed ? 'Done 🐾' : 'Ready';
        waitTimerLabel.textContent = completed ? 'hope the wait felt shorter' : 'choose a pause';
    };
    pauseButtons.forEach(button => button.addEventListener('click', () => {
        stopPauseTimer(false);
        const minutes = Number(button.dataset.pauseMinutes);
        if (!Number.isFinite(minutes) || minutes <= 0) return;
        pauseRemaining = minutes * 60;
        button.classList.add('active');
        stopPause.disabled = false;
        renderPause();
        pauseInterval = setInterval(() => {
            pauseRemaining -= 1;
            if (pauseRemaining <= 0) stopPauseTimer(true); else renderPause();
        }, 1000);
    }));
    stopPause.addEventListener('click', () => stopPauseTimer(false));

    /* Treat Catch */
    const catchBoard = $('catchBoard');
    const catchTarget = $('catchTarget');
    const catchIntro = $('catchIntro');
    const catchScoreEl = $('catchScore');
    const catchTimeEl = $('catchTime');
    const catchBestEl = $('catchBest');
    const catchMessage = $('catchMessage');
    const startCatch = $('startCatch');
    const treatIcons = ['🦴', '🎾', '🐾', '🥎', '🐟'];
    let catchTimer = null;
    let catchScore = 0;
    let catchTime = 20;
    let catchRunning = false;
    let catchBest = Number(safeStore.get('pawcare_catch_best', '0')) || 0;
    catchBestEl.textContent = String(catchBest);

    function moveCatchTarget() {
        const boardRect = catchBoard.getBoundingClientRect();
        const size = 58;
        const pad = 10;
        const maxX = Math.max(pad, boardRect.width - size - pad);
        const maxY = Math.max(pad, boardRect.height - size - pad);
        const left = pad + Math.random() * Math.max(1, maxX - pad);
        const top = pad + Math.random() * Math.max(1, maxY - pad);
        catchTarget.style.left = `${left}px`;
        catchTarget.style.top = `${top}px`;
        catchTarget.textContent = treatIcons[Math.floor(Math.random() * treatIcons.length)];
    }
    function finishCatch() {
        if (catchTimer) clearInterval(catchTimer);
        catchTimer = null;
        catchRunning = false;
        catchTarget.hidden = true;
        catchIntro.hidden = false;
        startCatch.disabled = false;
        startCatch.innerHTML = '<i class="bi bi-arrow-clockwise me-1"></i>Play again';
        if (catchScore > catchBest) {
            catchBest = catchScore;
            safeStore.set('pawcare_catch_best', catchBest);
            catchBestEl.textContent = String(catchBest);
            catchMessage.textContent = `New best: ${catchScore}! Tiny paws, quick reflexes.`;
        } else {
            catchMessage.textContent = `You caught ${catchScore}. Best on this browser: ${catchBest}.`;
        }
    }
    function beginCatch() {
        if (catchTimer) clearInterval(catchTimer);
        catchScore = 0;
        catchTime = 20;
        catchRunning = true;
        catchScoreEl.textContent = '0';
        catchTimeEl.textContent = '20';
        catchMessage.textContent = 'Go! Catch the moving treat.';
        catchIntro.hidden = true;
        catchTarget.hidden = false;
        startCatch.disabled = true;
        moveCatchTarget();
        catchTimer = setInterval(() => {
            catchTime -= 1;
            catchTimeEl.textContent = String(catchTime);
            if (catchTime <= 0) finishCatch();
        }, 1000);
    }
    startCatch.addEventListener('click', beginCatch);
    catchTarget.addEventListener('click', () => {
        if (!catchRunning) return;
        catchScore += 1;
        catchScoreEl.textContent = String(catchScore);
        moveCatchTarget();
    });
    window.addEventListener('resize', () => { if (catchRunning) moveCatchTarget(); });

    /* Paw Pairs */
    const pairBoard = $('pairBoard');
    const pairMovesEl = $('pairMoves');
    const pairMatchesEl = $('pairMatches');
    const pairBestEl = $('pairBest');
    const pairMessage = $('pairMessage');
    const newPairs = $('newPairs');
    const pairSymbols = ['🐶', '🐱', '🐰', '🐾', '🦴', '🎾'];
    let firstPair = null;
    let pairLock = false;
    let pairMoves = 0;
    let pairMatches = 0;
    let pairBoardVersion = 0;
    let pairBest = Number(safeStore.get('pawcare_pairs_best', '0')) || 0;
    pairBestEl.textContent = pairBest ? String(pairBest) : '—';

    function resetPairs() {
        pairBoardVersion += 1;
        firstPair = null;
        pairLock = false;
        pairMoves = 0;
        pairMatches = 0;
        pairMovesEl.textContent = '0';
        pairMatchesEl.textContent = '0/6';
        pairMessage.textContent = 'Find all six pairs.';
        const deck = shuffle([...pairSymbols, ...pairSymbols].map((symbol, index) => ({symbol, id: index})));
        pairBoard.innerHTML = '';
        deck.forEach((item, index) => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'pair-card';
            button.dataset.symbol = item.symbol;
            button.dataset.index = String(index);
            button.setAttribute('aria-label', `Hidden pet card ${index + 1}`);
            button.innerHTML = '<span class="pair-card-inner"><span class="pair-card-face pair-card-front">🐾</span><span class="pair-card-face pair-card-back"></span></span>';
            button.querySelector('.pair-card-back').textContent = item.symbol;
            button.addEventListener('click', () => flipPair(button));
            pairBoard.appendChild(button);
        });
    }
    function flipPair(card) {
        if (pairLock || card.classList.contains('is-flipped') || card.classList.contains('is-matched')) return;
        card.classList.add('is-flipped');
        card.setAttribute('aria-label', `Revealed ${card.dataset.symbol}`);
        if (!firstPair) { firstPair = card; return; }
        pairMoves += 1;
        pairMovesEl.textContent = String(pairMoves);
        if (firstPair.dataset.symbol === card.dataset.symbol) {
            firstPair.classList.add('is-matched');
            card.classList.add('is-matched');
            firstPair.disabled = true;
            card.disabled = true;
            pairMatches += 1;
            pairMatchesEl.textContent = `${pairMatches}/6`;
            firstPair = null;
            if (pairMatches === 6) {
                if (!pairBest || pairMoves < pairBest) {
                    pairBest = pairMoves;
                    safeStore.set('pawcare_pairs_best', pairBest);
                    pairBestEl.textContent = String(pairBest);
                    pairMessage.textContent = `Perfect! New best: ${pairMoves} moves.`;
                } else pairMessage.textContent = `All pairs found in ${pairMoves} moves. Best: ${pairBest}.`;
            } else pairMessage.textContent = 'Nice match! Keep going.';
            return;
        }
        pairLock = true;
        const previous = firstPair;
        const boardVersion = pairBoardVersion;
        firstPair = null;
        pairMessage.textContent = 'Not a pair — try another combination.';
        setTimeout(() => {
            if (boardVersion !== pairBoardVersion) return;
            previous.classList.remove('is-flipped');
            card.classList.remove('is-flipped');
            previous.setAttribute('aria-label', 'Hidden pet card');
            card.setAttribute('aria-label', 'Hidden pet card');
            pairLock = false;
        }, 650);
    }
    newPairs.addEventListener('click', resetPairs);
    resetPairs();

    /* Pet Trivia */
    const triviaQuestions = [
        {icon:'🐶', q:'Which sense is especially powerful in dogs?', options:['Smell','Taste','Color vision','Balance'], answer:0, note:'Dogs rely heavily on their sense of smell to understand the world around them.'},
        {icon:'🐱', q:'What can a cat’s whiskers help it do?', options:['Sense nearby spaces','Change fur color','Hear music better','Sleep longer'], answer:0, note:'Whiskers are sensitive and help cats judge nearby spaces and movement.'},
        {icon:'🐰', q:'What is special about a rabbit’s teeth?', options:['They grow continuously','They disappear in winter','They are all baby teeth','They never need chewing'], answer:0, note:'Rabbit teeth grow continuously, which is one reason appropriate chewing is important.'},
        {icon:'💧', q:'Why should pets have access to fresh water?', options:['For healthy hydration','To make them taller','To change eye color','Only after exercise'], answer:0, note:'Fresh water supports normal hydration throughout the day.'},
        {icon:'🐕', q:'Panting commonly helps many dogs to…', options:['Cool themselves','Grow fur','Improve night vision','Clean their paws'], answer:0, note:'Panting is one of the main ways dogs release body heat.'},
        {icon:'🐾', q:'If a pet seems uncomfortable with an interaction, a good first response is to…', options:['Give it space and observe','Keep holding it tightly','Make the room louder','Ignore all body language'], answer:0, note:'Respecting space and body language can help keep interactions calmer and safer.'},
        {icon:'🎾', q:'Regular play can give many pets…', options:['Mental stimulation and activity','A different species','Instant training certificates','A new coat color'], answer:0, note:'Play is a simple way to provide activity, enrichment and bonding time.'},
        {icon:'🧼', q:'Pet grooming can help owners notice…', options:['Changes in skin or coat','Tomorrow’s weather','A pet’s exact age','The color of its dreams'], answer:0, note:'Routine grooming gives you a chance to notice changes in the coat, skin, paws or ears.'}
    ];
    const triviaScoreEl = $('triviaScore');
    const triviaProgress = $('triviaProgress');
    const triviaBestEl = $('triviaBest');
    const triviaIcon = $('triviaIcon');
    const triviaQuestion = $('triviaQuestion');
    const triviaOptions = $('triviaOptions');
    const triviaFeedback = $('triviaFeedback');
    const nextTrivia = $('nextTrivia');
    const restartTrivia = $('restartTrivia');
    let triviaRound = [];
    let triviaIndex = 0;
    let triviaScore = 0;
    let triviaAnswered = false;
    let triviaBest = Number(safeStore.get('pawcare_trivia_best', '0')) || 0;
    triviaBestEl.textContent = `${triviaBest}/5`;

    function startTrivia() {
        triviaRound = shuffle(triviaQuestions).slice(0, 5);
        triviaIndex = 0;
        triviaScore = 0;
        triviaScoreEl.textContent = '0';
        renderTrivia();
    }
    function renderTrivia() {
        const item = triviaRound[triviaIndex];
        triviaAnswered = false;
        triviaProgress.textContent = `${triviaIndex + 1}/5`;
        triviaIcon.textContent = item.icon;
        triviaQuestion.textContent = item.q;
        triviaFeedback.textContent = '';
        nextTrivia.disabled = true;
        nextTrivia.innerHTML = triviaIndex === 4 ? 'See result <i class="bi bi-stars ms-1"></i>' : 'Next question <i class="bi bi-arrow-right ms-1"></i>';
        triviaOptions.innerHTML = '';
        const optionOrder = shuffle(item.options.map((text, originalIndex) => ({text, originalIndex})));
        optionOrder.forEach(option => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'trivia-option';
            button.textContent = option.text;
            button.dataset.originalIndex = String(option.originalIndex);
            button.addEventListener('click', () => answerTrivia(button, item));
            triviaOptions.appendChild(button);
        });
    }
    function answerTrivia(button, item) {
        if (triviaAnswered) return;
        triviaAnswered = true;
        const chosen = Number(button.dataset.originalIndex);
        const buttons = [...triviaOptions.querySelectorAll('.trivia-option')];
        buttons.forEach(b => {
            b.disabled = true;
            if (Number(b.dataset.originalIndex) === item.answer) b.classList.add('correct');
        });
        if (chosen === item.answer) {
            button.classList.add('correct');
            triviaScore += 1;
            triviaScoreEl.textContent = String(triviaScore);
            triviaFeedback.textContent = `Correct! ${item.note}`;
        } else {
            button.classList.add('wrong');
            triviaFeedback.textContent = `Good try. ${item.note}`;
        }
        nextTrivia.disabled = false;
    }
    nextTrivia.addEventListener('click', () => {
        if (!triviaAnswered) return;
        if (triviaIndex < 4) { triviaIndex += 1; renderTrivia(); return; }
        if (triviaScore > triviaBest) {
            triviaBest = triviaScore;
            safeStore.set('pawcare_trivia_best', triviaBest);
            triviaBestEl.textContent = `${triviaBest}/5`;
        }
        triviaIcon.textContent = triviaScore === 5 ? '🏆' : '🐾';
        triviaQuestion.textContent = `PawPause complete — you scored ${triviaScore} out of 5.`;
        triviaOptions.innerHTML = '';
        triviaFeedback.textContent = triviaScore >= 4 ? 'Great pet knowledge! Want another five?' : 'Nice little brain break. Try another round whenever you like.';
        nextTrivia.disabled = true;
        triviaProgress.textContent = '5/5';
    });
    restartTrivia.addEventListener('click', startTrivia);
    startTrivia();

    window.addEventListener('scroll', () => {
        document.getElementById('mainNavbar')?.classList.toggle('scrolled', window.scrollY > 50);
    });
})();
