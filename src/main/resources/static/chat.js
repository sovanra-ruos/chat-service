'use strict';

let stompClient = null;
let currentUser = null;
let currentRoom = null;

// DOM elements
const usernamePage = document.querySelector('#username-page');
const chatPage = document.querySelector('#chat-page');
const usernameForm = document.querySelector('#usernameForm');
const messageForm = document.querySelector('#messageForm');
const messageInput = document.querySelector('#message');
const messageArea = document.querySelector('#messageArea');
const connectingElement = document.querySelector('.connecting');
const roomSelect = document.querySelector('#roomSelect');
const roomNameElement = document.querySelector('#room-name');
const onlineCountElement = document.querySelector('#online-count');

let colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    loadRooms();
});

// Load available chat rooms
async function loadRooms() {
    try {
        const response = await fetch('/api/rooms');
        const rooms = await response.json();

        roomSelect.innerHTML = '<option value="">Select a room</option>';

        if (rooms.length === 0) {
            // Create a default room if none exist
            await createDefaultRoom();
            loadRooms(); // Reload after creating default room
            return;
        }

        rooms.forEach(room => {
            const option = document.createElement('option');
            option.value = room.id;
            option.textContent = room.name;
            roomSelect.appendChild(option);
        });
    } catch (error) {
        console.error('Error loading rooms:', error);
        // Create default room on error
        await createDefaultRoom();
    }
}

// Create a default room for testing
async function createDefaultRoom() {
    try {
        const response = await fetch('/api/rooms', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                name: 'General Chat',
                description: 'Default chat room for testing'
            })
        });

        if (response.ok) {
            console.log('Default room created successfully');
        }
    } catch (error) {
        console.error('Error creating default room:', error);
    }
}

// Handle username form submission
usernameForm.addEventListener('submit', function(event) {
    event.preventDefault();

    const username = document.querySelector('#name').value.trim();
    const selectedRoomId = roomSelect.value;

    if (username && selectedRoomId) {
        currentUser = {
            id: generateUserId(),
            name: username
        };
        currentRoom = {
            id: selectedRoomId,
            name: roomSelect.options[roomSelect.selectedIndex].text
        };

        usernamePage.classList.add('hidden');
        chatPage.classList.remove('hidden');
        roomNameElement.textContent = currentRoom.name;

        connect();
    } else {
        alert('Please enter your username and select a room');
    }
});

// Handle message form submission
messageForm.addEventListener('submit', function(event) {
    event.preventDefault();
    sendMessage();
});

// Generate a unique user ID
function generateUserId() {
    return 'user_' + Math.random().toString(36).substr(2, 9) + '_' + Date.now();
}

// Connect to WebSocket
function connect() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function(frame) {
        console.log('Connected: ' + frame);
        connectingElement.classList.add('hidden');

        // Subscribe to room messages
        stompClient.subscribe(`/topic/room/${currentRoom.id}`, function(message) {
            const messageData = JSON.parse(message.body);
            displayMessage(messageData);
        });

        // Subscribe to room presence updates
        stompClient.subscribe(`/topic/room/${currentRoom.id}/presence`, function(presence) {
            const presenceData = JSON.parse(presence.body);
            updateUserPresence(presenceData);
        });

        // Add user to the room
        stompClient.send(`/app/chat.addUser/${currentRoom.id}`, {}, JSON.stringify({
            userId: currentUser.id,
            username: currentUser.name
        }));

    }, function(error) {
        console.error('WebSocket connection error:', error);
        connectingElement.textContent = 'Could not connect to WebSocket server. Please refresh this page to try again!';
        connectingElement.style.color = 'red';
    });
}

// Send a message
function sendMessage() {
    const messageContent = messageInput.value.trim();

    if (messageContent && stompClient && stompClient.connected) {
        const message = {
            senderId: currentUser.id,
            senderName: currentUser.name,
            content: messageContent,
            type: 'CHAT'
        };

        stompClient.send(`/app/chat.sendMessage/${currentRoom.id}`, {}, JSON.stringify(message));
        messageInput.value = '';
    }
}

// Display a message in the chat area
function displayMessage(message) {
    const messageElement = document.createElement('li');

    if (message.messageType === 'JOIN') {
        messageElement.classList.add('event-message');
        messageElement.innerHTML = `
            <div class="message-content">
                <i class="fas fa-user-plus"></i>
                <span>${message.content}</span>
            </div>
            <div class="message-time">${formatTime(message.timestamp)}</div>
        `;
    } else if (message.messageType === 'LEAVE') {
        messageElement.classList.add('event-message');
        messageElement.innerHTML = `
            <div class="message-content">
                <i class="fas fa-user-minus"></i>
                <span>${message.content}</span>
            </div>
            <div class="message-time">${formatTime(message.timestamp)}</div>
        `;
    } else {
        messageElement.classList.add('chat-message');
        if (message.senderId === currentUser.id) {
            messageElement.classList.add('own-message');
        }

        messageElement.innerHTML = `
            <div class="message-header">
                <span class="username">${message.senderName}</span>
                <span class="message-time">${formatTime(message.timestamp)}</span>
            </div>
            <div class="message-content">${escapeHtml(message.content)}</div>
        `;
    }

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}

// Update user presence
function updateUserPresence(presence) {
    console.log('User presence update:', presence);
    // You can implement online user count updates here
    // For now, we'll just log it
}

// Format timestamp
function formatTime(timestamp) {
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

// Escape HTML to prevent XSS
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Handle page unload
window.addEventListener('beforeunload', function() {
    if (stompClient && stompClient.connected) {
        stompClient.disconnect();
    }
});

// Handle Enter key in message input
messageInput.addEventListener('keypress', function(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
});

