const navButtons = document.querySelectorAll('.nav-btn');
const sections = document.querySelectorAll('.section');

const loginForm = document.getElementById('login-form');
const transferForm = document.getElementById('transfer-form');
const logoutForm = document.getElementById('logout-form');

const loginStatus = document.getElementById('login-status');
const transferStatus = document.getElementById('transfer-status');
const logoutStatus = document.getElementById('logout-status');

function setActiveSection(target) {
    sections.forEach((section) => {
        section.classList.toggle('active', section.id === `${target}-section`);
    });

    navButtons.forEach((button) => {
        button.classList.toggle('active', button.dataset.section === target);
    });
}

navButtons.forEach((button) => {
    button.addEventListener('click', () => setActiveSection(button.dataset.section));
});

async function sendRequest(url, payload, statusElement) {
    statusElement.classList.remove('success', 'error', 'info');
    statusElement.textContent = 'Processing request...';
    statusElement.classList.add('visible', 'info');

    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(payload),
        });

        const responseText = await response.text();

        if (!response.ok) {
            throw new Error(responseText || 'Request failed');
        }

        statusElement.classList.remove('info');
        statusElement.classList.add('success');
        statusElement.textContent = responseText || 'Success';
    } catch (error) {
        statusElement.classList.remove('info');
        statusElement.classList.add('error');
        statusElement.textContent = error.message || 'Request failed';
    }
}

loginForm?.addEventListener('submit', (event) => {
    event.preventDefault();

    const payload = {
        userId: loginForm.querySelector('#login-userId').value.trim(),
        password: loginForm.querySelector('#login-password').value,
        country: loginForm.querySelector('#login-country').value,
    };

    sendRequest('/auth/login', payload, loginStatus);
});

transferForm?.addEventListener('submit', (event) => {
    event.preventDefault();

    const payload = {
        userId: transferForm.querySelector('#transfer-userId').value.trim(),
        country: transferForm.querySelector('#transfer-country').value,
        amount: Number(transferForm.querySelector('#transfer-amount').value),
    };

    sendRequest('/transfer', payload, transferStatus);
});

logoutForm?.addEventListener('submit', (event) => {
    event.preventDefault();

    const payload = {
        userId: logoutForm.querySelector('#logout-userId').value.trim(),
        country: logoutForm.querySelector('#logout-country').value,
        password: 'logout',
    };

    sendRequest('/auth/logout', payload, logoutStatus);
});
