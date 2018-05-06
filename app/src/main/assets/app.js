"use strict";
var game = new DontYouFillItGame();
var gui = new DontYouFillItCanvasGui(game, parseInt(localStorage.getItem('highscore'), 10) || 0);

var stats = new Stats();
stats.showPanel(0); // 0: fps, 1: ms
stats.dom.style.removeProperty('top');
stats.dom.style.position = 'fixed';
stats.dom.style.left = '0px';
stats.dom.style.bottom = '0px';
stats.dom.style.display = 'none';
document.body.appendChild( stats.dom );

function statsObserver(message) {
	if(message == 'beginStep')
		stats.begin();
	else if(message == 'endStep')
		stats.end();
}


function setDebugMode(enabled) {
	localStorage.setItem('debug', enabled);

	if (enabled) {
		stats.dom.style.display = 'block';
		gui.addObserver(statsObserver);
	} else {
		stats.dom.style.display = 'none';
		gui.removeObserver(statsObserver);
	}
}

function asBool(v) {
	return v === true || v ==='true';
}

var query_string = {};
window.location.search.substring(1).split('&').forEach(function(e) {
	var pair = e.split('=', 2);
	query_string[pair[0]] = (pair.length == 2) ? pair[1] : true;
});


if (query_string['debug'] !== undefined) {
	setDebugMode(asBool(query_string['debug']));
} else if (localStorage.getItem('debug') !== null) {
	setDebugMode(asBool(localStorage.getItem('debug')));
} else {
	setDebugMode(false);
}


var screenContainer = document.getElementById('screenContainer'),
        startScreen = document.getElementById('startScreen'),
      optionsScreen = document.getElementById('optionsScreen'),
        pauseScreen = document.getElementById('pauseScreen'),
     gameoverScreen = document.getElementById('gameoverScreen'),
      licenseScreen = document.getElementById('licenseScreen');

var screens = [];

function pushScreen(screen) {
	if (screens.length != 0) screens[screens.length - 1].style.display = 'none';

	screens.push(screen);
	screen.style.zIndex = screens.length;
	// Prevent flickering
	screen.style.visibility = 'hidden';
	screen.style.display = 'block';
	screen.scrollTop = 0;
	screen.style.visibility = 'visible';

	screenContainer.style.display = 'block';
	screenContainer.style.backgroundColor = (screen == pauseScreen) ? 'rgba(0, 0, 0, 0.85)' : 'black';
}

function popScreen() {
	if (screens.length == 0) return;

	screens.pop().style.display = 'none';

	if (screens.length == 0) {
		screenContainer.style.display = 'none';
	} else {
		screens[screens.length - 1].style.display = 'block';
	}
}

function popAllScreens() {
	while(screens.length > 0)
		screens.pop().style.display = 'none';

	screenContainer.style.display = 'none';
}

document.getElementById('startScreenPlayButton').addEventListener('click', function(evt) {
	evt.preventDefault();
	gui.resume();
	popAllScreens();
});

document.getElementById('startScreenOptionsButton').addEventListener('click', function(evt) {
	evt.preventDefault();
	optionsScreen.init();
	pushScreen(optionsScreen);
});

document.getElementById('optionsScreenBackButton').addEventListener('click', function(evt) {
	evt.preventDefault();
	popScreen();
});

document.getElementById('pauseScreenContinueButton').addEventListener('click', function(evt) {
	evt.preventDefault();
	gui.resume();
	popScreen();
});

document.getElementById('pauseScreenOptionsButton').addEventListener('click', function(evt) {
	evt.preventDefault();
	pushScreen(optionsScreen);
});

document.getElementById('gameoverScreenPlayAgainButton').addEventListener('click', function(evt) {
	evt.preventDefault();
	gui.reset();
	popAllScreens();
});

document.getElementById('startScreenLicenseButton').addEventListener('click', function(evt) {
	evt.preventDefault();
	pushScreen(licenseScreen);
});

document.getElementById('licenseScreenBackButton').addEventListener('click', function(evt) {
	evt.preventDefault();
	popScreen();
	licenseScreen.reset();
});

document.getElementById('framerateCheckbox').addEventListener('change', function(evt) {
	setDebugMode(evt.target.checked);
});

gui.addObserver(function(message) {
	if(message == 'pause') pushScreen(pauseScreen);
});

gui.addObserver(function(message, score) {
	if(message == 'gameover') {
		var highscore = parseInt(localStorage.getItem('highscore'), 10) || 0;
		var newHighscore = score > highscore;

		if (newHighscore) localStorage.setItem('highscore', score.toString(10));

		document.getElementById('gameoverScreenScoreMessage').style.display = (newHighscore ? 'none' : 'inline');
		document.getElementById('gameoverScreenHighscoreMessage').style.display = (newHighscore ? 'inline' : 'none');
		document.getElementById('gameoverScreenScore').innerHTML = score;

		pushScreen(gameoverScreen);
	}
});

pushScreen(startScreen);


function setNodeText(node, text) {
	var child = node.firstChild;
	do {
		if (3 == child.nodeType) {
			child.nodeValue = text;
			break;
		}
	} while (child = child.nextSibling);
}

Array.prototype.forEach.call(document.getElementsByClassName('hideable'), function(hideable) {
	var foreach_enablers = function(cbk) {
		Array.prototype.forEach.call(document.getElementsByClassName(hideable.getAttribute('data-toggle')), cbk);
	};

	hideable.reset = function() {
		this.style.display = 'none';
		foreach_enablers(function(e) {
			setNodeText(e, "[-]");
		});
	};

	foreach_enablers(function(enabler) {
		enabler.addEventListener('click', function(evt) {
			var visible = hideable.style.display != 'none';
			foreach_enablers(function(e) {
				setNodeText(e, visible ? "[+]" : "[-]");
			});
			hideable.style.display = visible ? 'none' : 'block';
			evt.preventDefault();
		});
	});

	hideable.style.display = 'none';
});

optionsScreen.init = function() {
	document.getElementById('framerateCheckbox').checked = gui.hasObserver(statsObserver);
};

licenseScreen.reset = function() {
	Array.prototype.forEach.call(this.getElementsByClassName('hideable'), function(hideable) {
		hideable.reset();
	});
};
