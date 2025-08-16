'use strict';

// Global variables
let stompClient = null;
let currentUser = null;
let accessToken = null;
let refreshToken = null;
let currentRoom = null;

// Page elements
const authPage = document.querySelector('#auth-page');
const roomPage = document.querySelector('#room-page');
const chatPage = document.querySelector('#chat-page');
const loginForm = document.querySelector('#login-form');
const registerForm = document.querySelector('#register-form');
const errorMessage = document.querySelector('#errorMessage');

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
    setupEventListeners();
});

function initializeApp() {
    // Check if user is already logged in
    const storedToken = localStorage.getItem('accessToken');
    const storedUser = localStorage.getItem('currentUser');

    if (storedToken && storedUser) {
        accessToken = storedToken;
        refreshToken = localStorage.getItem('refreshToken');
        currentUser = JSON.parse(storedUser);
        showRoomPage();
    } else {
        showAuthPage();
    }
}

function setupEventListeners() {
    // Authentication forms
    document.getElementById('loginForm').addEventListener('submit', handleLogin);
    document.getElementById('registerForm').addEventListener('submit', handleRegister);
    document.getElementById('showRegister').addEventListener('click', showRegisterForm);
    document.getElementById('showLogin').addEventListener('click', showLoginForm);

    // Room management
    document.getElementById('createRoomForm').addEventListener('submit', createRoom);
    document.getElementById('logoutBtn').addEventListener('click', logout);

    // Chat functionality
    document.getElementById('messageForm').addEventListener('submit', sendMessage);
    document.getElementById('leaveRoom').addEventListener('click', leaveRoom);

    // Error handling
    document.getElementById('closeError').addEventListener('click', hideError);
}

// Authentication Functions
async function handleLogin(event) {
    event.preventDefault();

    const email = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value;

    if (!email || !password) {
        showError('Please fill in all fields');
        return;
    }

    try {
        const response = await fetch('/api/v1/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ email, password })
        });

        if (response.ok) {
            const authData = await response.json();
            handleAuthSuccess(authData, { email });
        } else {
            const error = await response.text();
            showError('Login failed: ' + error);
        }
    } catch (error) {
        showError('Network error: ' + error.message);
    }
}

async function handleRegister(event) {
    event.preventDefault();

    const userName = document.getElementById('registerUsername').value.trim();
    const email = document.getElementById('registerEmail').value.trim();
    const password = document.getElementById('registerPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (!userName || !email || !password || !confirmPassword) {
        showError('Please fill in all fields');
        return;
    }

    if (password !== confirmPassword) {
        showError('Passwords do not match');
        return;
    }

    try {
        const response = await fetch('/api/v1/auth/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ userName, email, password, confirm_password: confirmPassword })
        });

        if (response.ok) {
            showError('Registration successful! Please login.', 'success');
            showLoginForm();
        } else {
            const error = await response.text();
            showError('Registration failed: ' + error);
        }
    } catch (error) {
        showError('Network error: ' + error.message);
    }
}

function handleAuthSuccess(authData, userData) {
    accessToken = authData.accessToken;
    refreshToken = authData.refreshToken;

    // Extract user information from the JWT token payload
    try {
        const payload = JSON.parse(atob(accessToken.split('.')[1]));
        currentUser = {
            id: payload.user_id, // Use the user_id claim you added to the JWT
            email: userData.email,
            username: userData.email.split('@')[0]
        };
    } catch (error) {
        // Fallback if JWT parsing fails
        currentUser = {
            id: generateUserId(),
            email: userData.email,
            username: userData.email.split('@')[0]
        };
    }

    // Store in localStorage
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('currentUser', JSON.stringify(currentUser));

    showRoomPage();
}

// Room Management Functions
async function loadRooms() {
    try {
        const response = await authenticatedFetch('/api/v1/rooms');
        if (response.ok) {
            const rooms = await response.json();
            displayRooms(rooms);
        } else {
            showError('Failed to load rooms');
        }
    } catch (error) {
        showError('Error loading rooms: ' + error.message);
    }
}

function displayRooms(rooms) {
    const roomsList = document.getElementById('roomsList');
    roomsList.innerHTML = '';

    if (rooms.length === 0) {
        roomsList.innerHTML = '<p>No rooms available. Create one!</p>';
        return;
    }

    rooms.forEach(room => {
        const roomElement = document.createElement('div');
        roomElement.className = 'room-item';
        roomElement.innerHTML = `
            <h4>${room.name}</h4>
            <p>${room.description || 'No description'}</p>
            <button onclick="joinRoom('${room.id}', '${room.name}')" class="join-room-btn">Join Room</button>
        `;
        roomsList.appendChild(roomElement);
    });
}

async function createRoom(event) {
    event.preventDefault();

    const name = document.getElementById('roomName').value.trim();
    const description = document.getElementById('roomDescription').value.trim();

    if (!name) {
        showError('Room name is required');
        return;
    }

    try {
        const response = await authenticatedFetch('/api/v1/rooms', {
            method: 'POST',
            body: JSON.stringify({ name, description })
        });

        if (response.ok) {
            const room = await response.json();
            document.getElementById('createRoomForm').reset();
            loadRooms(); // Refresh the room list
            showError('Room created successfully!', 'success');
        } else {
            showError('Failed to create room');
        }
    } catch (error) {
        showError('Error creating room: ' + error.message);
    }
}

function joinRoom(roomId, roomName) {
    currentRoom = { id: roomId, name: roomName };
    document.getElementById('room-name').textContent = roomName;
    showChatPage();
    connectToChat();
}

// WebSocket Chat Functions
function connectToChat() {
    if (stompClient && stompClient.connected) {
        stompClient.disconnect();
    }

    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    // Set authorization header for WebSocket connection
    const connectHeaders = {
        'Authorization': `Bearer ${accessToken}`
    };

    stompClient.connect(connectHeaders, onConnected, onError);
}

function onConnected() {
    // Subscribe to the public topic
    stompClient.subscribe(`/topic/room/${currentRoom.id}`, onMessageReceived);
    stompClient.subscribe(`/topic/room/${currentRoom.id}/presence`, onPresenceUpdate);

    // Tell your username to the server
    stompClient.send(`/app/chat.addUser/${currentRoom.id}`,
        { 'Authorization': `Bearer ${accessToken}` },
        JSON.stringify({
            userId: currentUser.id || generateUserId(),
            username: currentUser.email.split('@')[0]
        })
    );

    document.querySelector('.connecting').classList.add('hidden');
    loadRoomMessages();
}

function onError(error) {
    console.error('WebSocket connection error:', error);
    document.querySelector('.connecting').textContent = 'Could not connect to WebSocket server. Please refresh this page and try again!';
    document.querySelector('.connecting').style.color = 'red';
}

function sendMessage(event) {
    event.preventDefault();

    const messageContent = document.getElementById('message').value.trim();
    if (messageContent && stompClient) {
        const chatMessage = {
            senderId: currentUser.id || generateUserId(),
            senderName: currentUser.email.split('@')[0],
            content: messageContent
        };

        stompClient.send(`/app/chat.sendMessage/${currentRoom.id}`,
            { 'Authorization': `Bearer ${accessToken}` },
            JSON.stringify(chatMessage)
        );

        document.getElementById('message').value = '';
    }
}

function onMessageReceived(payload) {
    const message = JSON.parse(payload.body);

    const messageElement = document.createElement('li');
    messageElement.classList.add('chat-message');

    if (message.type === 'JOIN') {
        messageElement.classList.add('event-message');
        messageElement.innerHTML = `<span class="message-content">${message.content}</span>`;
    } else if (message.type === 'LEAVE') {
        messageElement.classList.add('event-message');
        messageElement.innerHTML = `<span class="message-content">${message.content}</span>`;
    } else {
        // Check if this message is from the current user
        const isMyMessage = message.senderId === currentUser.id ||
                           message.senderName === currentUser.username ||
                           message.senderName === currentUser.email.split('@')[0];

        if (isMyMessage) {
            messageElement.classList.add('my-message');
        } else {
            messageElement.classList.add('other-message');
        }

        messageElement.innerHTML = `
            <div class="message-bubble">
                <div class="message-header">
                    <span class="username">${message.senderName}</span>
                    <span class="timestamp">${formatTime(message.timestamp)}</span>
                </div>
                <div class="message-content">${message.content}</div>
            </div>
        `;
    }

    const messageArea = document.getElementById('messageArea');
    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}

function onPresenceUpdate(payload) {
    const presenceData = JSON.parse(payload.body);
    document.getElementById('online-count').textContent = presenceData.count || 0;
}

async function loadRoomMessages() {
    try {
        const response = await authenticatedFetch(`/api/v1/rooms/${currentRoom.id}/messages?limit=50`);
        if (response.ok) {
            const messages = await response.json();
            displayMessages(messages);
        }
    } catch (error) {
        console.error('Error loading messages:', error);
    }
}

function displayMessages(messages) {
    const messageArea = document.getElementById('messageArea');
    messageArea.innerHTML = '';

    messages.forEach(message => {
        onMessageReceived({ body: JSON.stringify(message) });
    });
}

// Utility Functions
async function authenticatedFetch(url, options = {}) {
    const defaultOptions = {
        headers: {
            'Authorization': `Bearer ${accessToken}`,
            'Content-Type': 'application/json',
            ...options.headers
        }
    };

    const response = await fetch(url, { ...options, ...defaultOptions });

    // Handle token refresh if needed
    if (response.status === 401 && refreshToken) {
        const refreshed = await refreshAccessToken();
        if (refreshed) {
            defaultOptions.headers['Authorization'] = `Bearer ${accessToken}`;
            return fetch(url, { ...options, ...defaultOptions });
        }
    }

    return response;
}

async function refreshAccessToken() {
    try {
        const response = await fetch('/api/v1/auth/refresh', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ refreshToken })
        });

        if (response.ok) {
            const authData = await response.json();
            accessToken = authData.accessToken;
            localStorage.setItem('accessToken', accessToken);
            return true;
        }
    } catch (error) {
        console.error('Token refresh failed:', error);
    }

    logout();
    return false;
}

function generateUserId() {
    return 'user_' + Math.random().toString(36).substr(2, 9);
}

function formatTime(timestamp) {
    if (!timestamp) return '';
    return new Date(timestamp).toLocaleTimeString();
}

// UI Navigation Functions
function showAuthPage() {
    authPage.classList.remove('hidden');
    roomPage.classList.add('hidden');
    chatPage.classList.add('hidden');
}

function showRoomPage() {
    authPage.classList.add('hidden');
    roomPage.classList.remove('hidden');
    chatPage.classList.add('hidden');

    document.getElementById('currentUser').textContent = currentUser.email.split('@')[0];
    loadRooms();
}

function showChatPage() {
    authPage.classList.add('hidden');
    roomPage.classList.add('hidden');
    chatPage.classList.remove('hidden');

    document.querySelector('.connecting').classList.remove('hidden');
}

function showLoginForm() {
    loginForm.classList.remove('hidden');
    registerForm.classList.add('hidden');
}

function showRegisterForm() {
    loginForm.classList.add('hidden');
    registerForm.classList.remove('hidden');
}

function leaveRoom() {
    if (stompClient) {
        stompClient.disconnect();
    }
    currentRoom = null;
    showRoomPage();
}

function logout() {
    if (stompClient) {
        stompClient.disconnect();
    }

    // Clear stored data
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('currentUser');

    accessToken = null;
    refreshToken = null;
    currentUser = null;
    currentRoom = null;

    showAuthPage();
}

function showError(message, type = 'error') {
    const errorText = document.getElementById('errorText');
    errorText.textContent = message;

    errorMessage.className = `error-message ${type}`;
    errorMessage.classList.remove('hidden');

    // Auto-hide success messages after 3 seconds
    if (type === 'success') {
        setTimeout(hideError, 3000);
    }
}

function hideError() {
    errorMessage.classList.add('hidden');
}
