import { AuthService } from '../services/auth.js';


let allUsers = [];
let filteredUsers = [];
let currentPage = 1;
const usersPerPage = 12;

async function loadAllUsers() {
    try {
        console.log('Pokušavam da učitam korisnike...');
        const response = await fetch('/trivia/admin/superadmin/users');

        console.log('Response status:', response.status);
        console.log('Response ok:', response.ok);

        if (!response.ok) {
            const errorText = await response.text();
            console.error('Server response:', errorText);
            throw new Error(`Greška pri učitavanju korisnika: ${response.status}`);
        }

        const users = await response.json();
        console.log('Učitani korisnici:', users);
        
        allUsers = users;
        filteredUsers = [...users];
        
        updateUserCount();
        displayUsers();
        setupPagination();

    } catch (error) {
        console.error('Greška pri učitavanju korisnika:', error);
        showError('Nije moguće učitati korisnike. Molimo pokušajte ponovo. ' + error.message);
    }
}

function updateUserCount() {
    const countElement = document.getElementById('userCount');
    if (countElement) {
        countElement.textContent = `Ukupno korisnika: ${filteredUsers.length}`;
    }
}

function displayUsers() {
    const usersGrid = document.getElementById('usersGrid');
    if (!usersGrid) {
        console.error('usersGrid element not found');
        return;
    }
    
    usersGrid.innerHTML = '';

    if (filteredUsers.length === 0) {
        usersGrid.innerHTML = `
            <div class="empty-statistics" style="grid-column: 1 / -1;">
                <i class="material-icons">people</i>
                <h3>Nema korisnika</h3>
                <p>Nisu pronađeni korisnici koji odgovaraju kriterijumima pretrage.</p>
            </div>
        `;
        const paginationContainer = document.getElementById('paginationContainer');
        if (paginationContainer) {
            paginationContainer.style.display = 'none';
        }
        return;
    }

    
    const startIndex = (currentPage - 1) * usersPerPage;
    const endIndex = startIndex + usersPerPage;
    const usersToShow = filteredUsers.slice(startIndex, endIndex);

    usersToShow.forEach(user => {
        const userCard = createUserCard(user);
        usersGrid.appendChild(userCard);
    });

    setupPagination();
}

function createUserCard(user) {
    const userCard = document.createElement('div');
    userCard.className = 'quiz-card';

    
    const initials = `${user.firstName?.charAt(0) || ''}${user.lastName?.charAt(0) || ''}`.toUpperCase();
    
    
    const createdDate = new Date(user.createdAt).toLocaleDateString('sr-RS');

    userCard.innerHTML = `
        <div class="quiz-image">
            <div class="user-initials">
                ${initials}
            </div>
        </div>
        <div class="quiz-content">
            <span class="quiz-category ${user.isVerified ? 'verified' : 'unverified'}">
                ${user.isVerified ? 'Verifikovan' : 'Neverifikovan'}
            </span>
            <h3 class="quiz-title">${user.fullName || user.firstName + ' ' + user.lastName}</h3>
            <p class="quiz-description">@${user.username} • ${user.email}</p>
            <div class="quiz-creator">
                <i class="material-icons">calendar_today</i>
                <span>Registrovan: ${createdDate}</span>
            </div>
            <div class="quiz-meta">
                <span class="question-count">
                    <i class="material-icons">badge</i>
                    ID: ${user.id}
                </span>
                <span class="quiz-status ${user.isVerified ? 'active' : 'inactive'}">
                    <i class="material-icons">${user.isVerified ? 'verified' : 'pending'}</i>
                    ${user.isVerified ? 'Verifikovan' : 'Pending'}
                </span>
            </div>
            <div class="quiz-actions user-actions">
                <button class="btn-details" onclick="viewUserDetails(${user.id})">
                    <i class="material-icons">visibility</i>
                    Detalji
                </button>
                <button class="btn-edit" onclick="editUser(${user.id})">
                    <i class="material-icons">edit</i>
                </button>
                <button class="btn-delete" onclick="deleteUser(${user.id})">
                    <i class="material-icons">delete</i>
                </button>
            </div>
        </div>
    `;

    return userCard;
}

function setupPagination() {
    const totalPages = Math.ceil(filteredUsers.length / usersPerPage);
    const paginationContainer = document.getElementById('paginationContainer');
    const pageInfo = document.getElementById('pageInfo');
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');

    if (!paginationContainer) return;

    if (totalPages <= 1) {
        paginationContainer.style.display = 'none';
        return;
    }

    paginationContainer.style.display = 'flex';
    if (pageInfo) {
        pageInfo.textContent = `Stranica ${currentPage} od ${totalPages}`;
    }
    
    if (prevBtn) prevBtn.disabled = currentPage === 1;
    if (nextBtn) nextBtn.disabled = currentPage === totalPages;
}

function changePage(direction) {
    const totalPages = Math.ceil(filteredUsers.length / usersPerPage);
    
    if (direction === 1 && currentPage < totalPages) {
        currentPage++;
    } else if (direction === -1 && currentPage > 1) {
        currentPage--;
    }
    
    displayUsers();
    
    
    const quizzesSection = document.querySelector('.quizzes-section');
    if (quizzesSection) {
        quizzesSection.scrollIntoView({ 
            behavior: 'smooth', 
            block: 'start' 
        });
    }
}

function filterUsers() {
    const searchInput = document.getElementById('searchInput');
    const verificationFilter = document.getElementById('verificationFilter');
    const sortFilter = document.getElementById('sortFilter');

    const searchTerm = searchInput ? searchInput.value.toLowerCase() : '';
    const verificationValue = verificationFilter ? verificationFilter.value : '';
    const sortValue = sortFilter ? sortFilter.value : '';

    filteredUsers = allUsers.filter(user => {
        const matchesSearch = user.fullName?.toLowerCase().includes(searchTerm) ||
                            user.username?.toLowerCase().includes(searchTerm) ||
                            user.email?.toLowerCase().includes(searchTerm);
        
        const matchesVerification = !verificationValue || 
                                  (verificationValue === 'verified' && user.isVerified) ||
                                  (verificationValue === 'unverified' && !user.isVerified);

        return matchesSearch && matchesVerification;
    });

    
    if (sortValue === 'newest') {
        filteredUsers.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    } else if (sortValue === 'oldest') {
        filteredUsers.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
    } else if (sortValue === 'alphabetical') {
        filteredUsers.sort((a, b) => (a.fullName || '').localeCompare(b.fullName || ''));
    }

    currentPage = 1; 
    updateUserCount();
    displayUsers();
}

function viewUserDetails(userId) {
    if (event) event.preventDefault();
    console.log(`Prikazuju se detalji korisnika sa ID: ${userId}`);
    
    
    const user = allUsers.find(u => u.id === userId);
    if (!user) {
        console.error('Korisnik nije pronađen');
        return;
    }
    
    showUserDetailsModal(user);
}

function showUserDetailsModal(user) {
    
    let modalOverlay = document.getElementById('user-details-modal');
    if (!modalOverlay) {
        modalOverlay = document.createElement('div');
        modalOverlay.id = 'user-details-modal';
        modalOverlay.className = 'modal-overlay';
        document.body.appendChild(modalOverlay);
    }

    const initials = `${user.firstName?.charAt(0) || ''}${user.lastName?.charAt(0) || ''}`.toUpperCase();
    const createdDate = new Date(user.createdAt).toLocaleDateString('bs-BA');
    const updatedDate = new Date(user.updatedAt).toLocaleDateString('bs-BA');

    modalOverlay.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h3>Detalji korisnika</h3>
                <button class="modal-close" onclick="closeUserDetailsModal()">
                    <i class="material-icons">close</i>
                </button>
            </div>
            <div class="modal-body">
                <div style="margin-bottom: 25px; text-align: center;">
                    <div class="user-initials-large">
                        ${initials}
                    </div>
                </div>
                
                <div style="margin-bottom: 20px;">
                    <span class="quiz-category ${user.isVerified ? 'verified' : 'unverified'}" style="display: inline-block; padding: 6px 15px; border-radius: 15px; font-size: 0.9rem; margin-bottom: 15px;">
                        ${user.isVerified ? 'Verifikovan korisnik' : 'Neverifikovan korisnik'}
                    </span>
                    <h2 style="color: var(--text-light); font-size: 1.8rem; font-weight: 500; margin-bottom: 15px;">
                        ${user.fullName || user.firstName + ' ' + user.lastName}
                    </h2>
                    <p style="color: rgba(255, 255, 255, 0.8); font-size: 1rem; line-height: 1.6; margin-bottom: 25px;">
                        @${user.username} • ${user.email}
                    </p>
                </div>

                <div class="stats-summary">
                    <div class="stat-card">
                        <div class="stat-value">${user.id}</div>
                        <div class="stat-label">Korisnički ID</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-value">${createdDate}</div>
                        <div class="stat-label">Datum registracije</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-value">${updatedDate}</div>
                        <div class="stat-label">Zadnja izmjena</div>
                    </div>
                </div>
            </div>
        </div>
    `;

    
    modalOverlay.classList.add('active');
    
    
    modalOverlay.onclick = function(e) {
        if (e.target === modalOverlay) {
            closeUserDetailsModal();
        }
    };
}

function closeUserDetailsModal() {
    const modalOverlay = document.getElementById('user-details-modal');
    if (modalOverlay) {
        modalOverlay.classList.remove('active');
    }
}

function editUser(userId) {
    if (event) event.preventDefault();
    console.log(`Editovanje korisnika sa ID: ${userId}`);
    
    
    const user = allUsers.find(u => u.id === userId);
    if (!user) {
        console.error('Korisnik nije pronađen');
        return;
    }
    
    showEditUserModal(user);
}

function showEditUserModal(user) {
    
    let modalOverlay = document.getElementById('edit-user-modal');
    if (!modalOverlay) {
        modalOverlay = document.createElement('div');
        modalOverlay.id = 'edit-user-modal';
        modalOverlay.className = 'modal-overlay';
        document.body.appendChild(modalOverlay);
    }

    const initials = `${user.firstName?.charAt(0) || ''}${user.lastName?.charAt(0) || ''}`.toUpperCase();

    modalOverlay.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h3>Uredi korisnika</h3>
                <button class="modal-close" onclick="closeEditUserModal()">
                    <i class="material-icons">close</i>
                </button>
            </div>
            <div class="modal-body">
                <form id="editUserForm">
                    <div style="margin-bottom: 25px; text-align: center;">
                        <div class="user-initials-large">
                            ${initials}
                        </div>
                    </div>
                    
                    <div class="edit-form-group">
                        <label for="editUsername" class="edit-form-label">Korisničko ime:</label>
                        <input type="text" id="editUsername" name="username" value="${user.username || ''}" class="edit-form-input" required>
                    </div>
                    
                    <div class="edit-form-group">
                        <label for="editEmail" class="edit-form-label">Email:</label>
                        <input type="email" id="editEmail" name="email" value="${user.email || ''}" class="edit-form-input" required>
                    </div>
                    
                    <div class="edit-form-group">
                        <label for="editFirstName" class="edit-form-label">Ime:</label>
                        <input type="text" id="editFirstName" name="firstName" value="${user.firstName || ''}" class="edit-form-input" required>
                    </div>
                    
                    <div class="edit-form-group">
                        <label for="editLastName" class="edit-form-label">Prezime:</label>
                        <input type="text" id="editLastName" name="lastName" value="${user.lastName || ''}" class="edit-form-input" required>
                    </div>
                    
                    <div class="edit-form-group">
                        <label class="edit-checkbox-container">
                            <input type="checkbox" id="editIsVerified" name="isVerified" ${user.isVerified ? 'checked' : ''} class="edit-checkbox">
                            <span class="edit-checkmark"></span>
                            Verifikovan korisnik
                        </label>
                    </div>
                    
                    <div class="modal-actions">
                        <button type="button" class="btn-cancel" onclick="closeEditUserModal()">
                            Otkaži
                        </button>
                        <button type="submit" class="btn-save">
                            <i class="material-icons">save</i>
                            Sačuvaj izmjene
                        </button>
                    </div>
                </form>
            </div>
        </div>
    `;

    
    modalOverlay.classList.add('active');
    
    
    modalOverlay.onclick = function(e) {
        if (e.target === modalOverlay) {
            closeEditUserModal();
        }
    };

    
    const form = document.getElementById('editUserForm');
    if (form) {
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            updateUser(user.id);
        });
    }
}

function closeEditUserModal() {
    const modalOverlay = document.getElementById('edit-user-modal');
    if (modalOverlay) {
        modalOverlay.classList.remove('active');
    }
}

async function updateUser(userId) {
    const form = document.getElementById('editUserForm');
    if (!form) return;

    const formData = new FormData(form);
    
    
    const userData = {
        id: userId,
        username: formData.get('username'),
        email: formData.get('email'),
        firstName: formData.get('firstName'),
        lastName: formData.get('lastName'),
        isVerified: formData.get('isVerified') === 'on' 
    };

    try {
        
        const submitBtn = form.querySelector('button[type="submit"]');
        const originalText = submitBtn.innerHTML;
        submitBtn.innerHTML = '<i class="material-icons">hourglass_empty</i> Čuva se...';
        submitBtn.disabled = true;

        const response = await fetch('/trivia/admin/superadmin/users', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(userData)
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Greška pri ažuriranju korisnika');
        }

        const updatedUser = await response.json();
        
        
        const userIndex = allUsers.findIndex(u => u.id === userId);
        if (userIndex !== -1) {
            allUsers[userIndex] = {
                ...allUsers[userIndex],
                ...updatedUser,
                fullName: `${updatedUser.firstName} ${updatedUser.lastName}`
            };
        }

        
        const filteredUserIndex = filteredUsers.findIndex(u => u.id === userId);
        if (filteredUserIndex !== -1) {
            filteredUsers[filteredUserIndex] = allUsers[userIndex];
        }

        
        displayUsers();
        
        
        closeEditUserModal();
        
        
        showSuccessMessage('Korisnik je uspješno ažuriran!');

    } catch (error) {
        console.error('Greška pri ažuriranju korisnika:', error);
        showError(`Greška pri ažuriranju korisnika: ${error.message}`);
    } finally {
        
        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.innerHTML = '<i class="material-icons">save</i> Sačuvaj izmjene';
            submitBtn.disabled = false;
        }
    }
}

async function deleteUser(userId) {
    if (event) event.preventDefault();
    console.log(`Brisanje korisnika sa ID: ${userId}`);
    
    const user = allUsers.find(u => u.id === userId);
    if (!user) {
        console.error('Korisnik nije pronađen');
        return;
    }
    
    if (confirm(`Da li ste sigurni da želite da obrišete korisnika ${user.fullName || user.firstName + ' ' + user.lastName}?`)) {
        try {
            const response = await fetch(`/trivia/admin/superadmin/users?id=${userId}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.error || 'Greška pri brisanju korisnika');
            }

            
            allUsers = allUsers.filter(u => u.id !== userId);
            filteredUsers = filteredUsers.filter(u => u.id !== userId);

            
            updateUserCount();
            displayUsers();
            
            showSuccessMessage('Korisnik je uspješno obrisan!');

        } catch (error) {
            console.error('Greška pri brisanju korisnika:', error);
            showError(`Greška pri brisanju korisnika: ${error.message}`);
        }
    }
}

function showSuccessMessage(message) {
    
    if (!document.getElementById('notification-styles')) {
        const style = document.createElement('style');
        style.id = 'notification-styles';
        style.textContent = `
            @keyframes slideIn {
                from { transform: translateX(100%); opacity: 0; }
                to { transform: translateX(0); opacity: 1; }
            }
            
            @keyframes slideOut {
                from { transform: translateX(0); opacity: 1; }
                to { transform: translateX(100%); opacity: 0; }
            }
        `;
        document.head.appendChild(style);
    }

    const notification = document.createElement('div');
    notification.className = 'success-notification';
    notification.innerHTML = `
        <i class="material-icons">check_circle</i>
        <span>${message}</span>
    `;
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: #4CAF50;
        color: white;
        padding: 15px 20px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.2);
        z-index: 10000;
        display: flex;
        align-items: center;
        gap: 8px;
        animation: slideIn 0.3s ease;
    `;

    document.body.appendChild(notification);

    
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 300);
    }, 3000);
}

function showError(message) {
    alert(message);
}

document.addEventListener('DOMContentLoaded', () => {
    console.log("DOM loaded, starting to load users...");
    loadAllUsers();
});

window.viewUserDetails = viewUserDetails;
window.closeUserDetailsModal = closeUserDetailsModal;
window.changePage = changePage;
window.filterUsers = filterUsers;
window.editUser = editUser;
window.deleteUser = deleteUser;
window.closeEditUserModal = closeEditUserModal;