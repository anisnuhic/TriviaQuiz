document.addEventListener('DOMContentLoaded', function() {
    
    const urlParams = new URLSearchParams(window.location.search);
    const isHost = urlParams.get('isHost') === 'true';
    const sessionPin = urlParams.get('sessionPin');
    
    
    const exportSection = document.getElementById('exportSection');
    if (isHost && exportSection) {
        exportSection.style.display = 'block';
        
        const exportBtn = document.getElementById('exportBtn');
        if (exportBtn) {
            exportBtn.addEventListener('click', exportToExcel);
        }
    }
    
    loadScoresData(sessionPin).catch(error => {
        console.error("Error in loadScoresData:", error);
    });
});

async function loadScoresData(sessionPin) {
    try {
        const tableBody = document.getElementById('scoresTableBody');
        tableBody.innerHTML = `
            <div class="loading-state">
                <i class="material-icons spinning">refresh</i>
                <span>Učitavanje rezultata...</span>
            </div>
        `;
        
        const response = await fetch(`/trivia/scores?sessionPin=${sessionPin}`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data.error) {
            throw new Error(data.error);
        }
        
        const sortedParticipants = data.participants.sort((a, b) => b.totalScore - a.totalScore);
        
        if (data.quizTitle) {
            document.querySelector('.scores-header h1').textContent = `Rezultati - ${data.quizTitle}`;
        }
        
        populateScoresTable(sortedParticipants);
        
        window.scoresData = {
            sessionPin: data.sessionPin,
            quizTitle: data.quizTitle,
            participants: sortedParticipants
        };
        
    } catch (error) {
        console.error('Error loading scores:', error);
        
        const tableBody = document.getElementById('scoresTableBody');
        tableBody.innerHTML = `
            <div class="error-state">
                <i class="material-icons">error_outline</i>
                <span>Greška pri učitavanju rezultata</span>
                <button onclick="location.reload()" class="btn-secondary">
                    <i class="material-icons">refresh</i>
                    Pokušaj ponovo
                </button>
            </div>
        `;
    }
}

function populateScoresTable(participants) {
    const tableBody = document.getElementById('scoresTableBody');
    
    if (!participants || participants.length === 0) {
        tableBody.innerHTML = `
            <div class="empty-state">
                <i class="material-icons">people_outline</i>
                <span>Nema učesnika u ovoj sesiji</span>
            </div>
        `;
        return;
    }
    
    let tableHTML = '';
    
    participants.forEach((participant, index) => {
        const rank = index + 1;
        const avatar = participant.username.charAt(0).toUpperCase();
        const rankClass = rank <= 3 ? `rank-${rank}` : '';
        
        tableHTML += `
            <div class="table-row">
                <div class="rank-cell">
                    <div class="rank-number ${rankClass}">${rank}</div>
                </div>
                <div class="name-cell">
                    <div class="participant-avatar">${avatar}</div>
                    <span class="participant-name">${participant.username}</span>
                    <div class="participant-stats">
                        <span class="stat">${participant.correctAnswers}/${participant.totalAnswers}</span>
                        <span class="stat-label">tačnih</span>
                        ${participant.accuracy ? `<span class="accuracy">${participant.accuracy}%</span>` : ''}
                    </div>
                </div>
                <div class="score-cell">
                    <span class="score-points">${participant.totalScore}</span>
                    <span class="score-label">bodova</span>
                </div>
            </div>
        `;
    });
    
    tableBody.innerHTML = tableHTML;
}

function goBack() {
     const urlParams = new URLSearchParams(window.location.search);
    const isHost = urlParams.get('isHost') === 'true';
    window.location.href = isHost ? "/trivia/admin/dashboard.html" : "/trivia" 
}

function exportToExcel() {
    try {
        if (!window.scoresData || !window.scoresData.participants) {
            return;
        }
        
        const data = window.scoresData;
        let csvContent = "Pozicija,Ime,Bodovi,Tačni odgovori,Ukupno pitanja,Tačnost\n";
        
        data.participants.forEach((participant, index) => {
            const rank = index + 1;
            const accuracy = participant.accuracy || 0;
            
            csvContent += `${rank},"${participant.username}",${participant.totalScore},${participant.correctAnswers},${participant.totalAnswers},${accuracy}%\n`;
        });
        
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        const url = URL.createObjectURL(blob);
        
        const fileName = `rezultati_${data.quizTitle || 'kviz'}_${data.sessionPin}_${new Date().toISOString().split('T')[0]}.csv`;
        
        link.setAttribute('href', url);
        link.setAttribute('download', fileName);
        link.style.visibility = 'hidden';
        
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        
        
    } catch (error) {
        console.error('Error exporting to Excel:', error);
    }
}

const style = document.createElement('style');
style.textContent = `
    @keyframes slideInRight {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOutRight {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
    
    @keyframes spin {
        from { transform: rotate(0deg); }
        to { transform: rotate(360deg); }
    }
    
    .spinning {
        animation: spin 1s linear infinite;
    }
    
    .loading-state, .error-state, .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 40px 20px;
        color: var(--text-secondary);
        gap: 15px;
    }
    
    .loading-state i, .error-state i, .empty-state i {
        font-size: 48px;
        opacity: 0.6;
    }
    
    .participant-stats {
        display: flex;
        gap: 8px;
        align-items: center;
        margin-top: 4px;
        font-size: 0.85em;
        color: var(--text-secondary);
    }
    
    .accuracy {
        background: rgba(78, 205, 196, 0.2);
        color: rgb(78, 205, 196);
        padding: 2px 6px;
        border-radius: 4px;
        font-weight: 500;
        font-size: 0.9em;
    }
`;
document.head.appendChild(style);