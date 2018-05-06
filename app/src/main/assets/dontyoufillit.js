"use strict";
// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Math/sign#Polyfill
if(!Math.sign) {
	Math.__proto__.sign = function(x) {
		if(isNaN(x)) {
			return NaN;
		} else if(x === 0) {
			return x;
		} else {
			return (x > 0 ? 1 : -1);
		}
	}
}

function vectorLength(x, y) {
	return Math.sqrt(x * x + y * y);
}

function RK41DObject_State(u, s) {
	this.u = u;
	this.s = s;
}

function RK41DObject_Derivative(du, ds) {
	this.du = du;
	this.ds = ds;
}

function RK41DObject() {
	this.state = new RK41DObject_State(0, 0);
}

RK41DObject.prototype.evaluate = function(initialState, t, dt, derivative) {
	if(typeof dt == 'undefined') {
		return new RK41DObject_Derivative(initialState.s, this.acceleration(initialState, t));
	} else {
		var state = new RK41DObject_State(initialState.u + derivative.du * dt,
		                                  initialState.s + derivative.ds * dt);

		return new RK41DObject_Derivative(state.s, this.acceleration(state, t + dt));
	}
};

RK41DObject.prototype.integrate = function(t, dt) {
	var a = this.evaluate(this.state, t),
	    b = this.evaluate(this.state, t, dt * 0.5, a),
	    c = this.evaluate(this.state, t, dt * 0.5, b),
	    d = this.evaluate(this.state, t, dt, c);

	var dxdt = 1/6 * (a.du + 2 * (b.du + c.du) + d.du),
	    dvdt = 1/6 * (a.ds + 2 * (b.ds + c.ds) + d.ds);

	this.state.u = this.state.u + dxdt * dt;
	this.state.s = this.state.s + dvdt * dt;
};

// TODO Improve this
function normalizeRadian(a) {
	while(a > 2 * Math.PI) {
		a -= 2 * Math.PI;
	}

	while(a < 0) {
		a += 2 * Math.PI;
	}

	return a;
}

function Cannon() {
	RK41DObject.call(this);
	this.state.u = 0;
	this.state.s = Math.PI / 3;
}

Cannon.prototype = new RK41DObject();
Cannon.prototype.constructor = RK41DObject;

Cannon.prototype.acceleration = function(state, t) {
	return 0;
};

Cannon.prototype.getAngle = function() {
	return this.state.u + Math.PI / 2;
};

Cannon.prototype.update = function(t, dt) {
	this.integrate(t, dt);

	if(Math.abs(this.state.u) >= Math.PI / 2) {
		this.state.u = ((Math.PI / 2) - Math.abs(Math.PI / 2 - Math.abs(this.state.u))) * Math.sign(this.state.u);
		this.state.s *= -1;
	}
};

function Ball(r, x, y, a) {
	RK41DObject.call(this);
	this.nr = r; // Normalized radius and coordinates
	this.nx = x;
	this.ny = y;

	this.direction = a;
	this.state.u = 0;
	this.state.s = 1;

	this.counter = 3;
}

Ball.prototype = new RK41DObject();
Ball.prototype.constructor = RK41DObject;

Ball.prototype.acceleration = function(state, t) {
	return -0.4;
};

Ball.prototype.update = function(t, dt, staticBalls) {
	var previousStateU = this.state.u;

	this.integrate(t, dt);

	var d = this.state.u - previousStateU;
	this.nx += d * Math.cos(this.direction);
	this.ny += d * Math.sin(this.direction);

	this.bounce(staticBalls);
};

Ball.prototype.bounce = function(staticBalls) {
	if (this.nx > 1 - this.nr) {
		this.nx = 1 - this.nr;
		this.direction = normalizeRadian(Math.PI - this.direction);
	} else if (this.nx < this.nr) {
		this.nx = this.nr;
		this.direction = normalizeRadian(Math.PI - this.direction);
	}

	if (this.ny > 1 - this.nr) {
		this.ny = 1 - this.nr;
		this.direction = normalizeRadian(-this.direction);
	}

	for(var i = 0; i < staticBalls.length; ++i) {
		var o = staticBalls[i];

		var normalX = this.nx - o.nx,
			normalY = this.ny - o.ny,
			dist = vectorLength(normalX, normalY);

		if(dist <= o.nr + this.nr) {
			--o.counter;

			// Move it back to prevent clipping
			this.nx = o.nx + normalX * (this.nr + o.nr) / dist;
			this.ny = o.ny + normalY * (this.nr + o.nr) / dist;

			// http://en.wikipedia.org/wiki/Elastic_collision#Two-Dimensional_Collision_With_Two_Moving_Objects
			// Assuming no speed and an infinite mass for the second ball.
			var phi = Math.atan2(normalY, normalX),
			  theta = this.direction,
			  speed = this.state.s;

			var velocityX = -speed * Math.cos(theta - phi) * Math.cos(phi) + speed * Math.sin(theta - phi) * Math.cos(phi + Math.PI / 2),
			    velocityY = -speed * Math.cos(theta - phi) * Math.sin(phi) + speed * Math.sin(theta - phi) * Math.sin(phi + Math.PI / 2);

			// Linear speed doesn't change, only the direction.
			this.direction = Math.atan2(velocityY, velocityX);
		}
	}
};

Ball.prototype.grow = function(staticBalls) {
	var minRadius = Number.MAX_VALUE,
	    available,
	    o;

	for(var i = 0; i < staticBalls.length; ++i) {
		o = staticBalls[i];
		available = vectorLength(this.nx - o.nx, this.ny - o.ny) - o.nr;
		if(minRadius > available) minRadius = available;
	}

	available = this.nx;
	if(minRadius > available) minRadius = available;

	available = 1 - this.nx;
	if(minRadius > available) minRadius = available;

	available = Math.abs(this.ny);
	if(minRadius > available) minRadius = available;

	available = Math.abs(1 - this.ny);
	if(minRadius > available) minRadius = available;

	this.nr = Math.abs(minRadius);
};

function DontYouFillItGame() {
	this.state = this.PAUSED();
	this.cannon = new Cannon();
	this.staticBalls = [];
	this.currentBall = null;

	// Browsers supporting high resolution timestamps will use them in requestAnimationFrame
	this.lastUpdateTime = performance.now ? performance.now() : Date.now();

	this.score = 0;

	this.DEFAULT_BALL_RADIUS =  1/40.0;
	this.CANNON_Y_POSITION   = -1/6.0;
	this.CANNON_BASE_HEIGHT  =  1/15.0;
	this.CANNON_LENGTH       =  1/15.0;
}

DontYouFillItGame.prototype.PAUSED   = function() { return 1; };
DontYouFillItGame.prototype.RUNNING  = function() { return 2; };
DontYouFillItGame.prototype.GAMEOVER = function() { return 3; };

/*
 * Position of the current ball is important, so it will be calculated 1000 times per second.
 * Position of the cannon isn't, so it will be calculated only once every frame.
 */
DontYouFillItGame.prototype.update = function(time) {
	if(this.currentBall) {
		var last = this.lastUpdateTime,
			steps = Math.floor(time - this.lastUpdateTime),
			current;
		for(var i = 1; i <= steps; ++i) {
			current = (this.lastUpdateTime * (steps-i) + time * i) / steps;
			this.currentBall.update(last / 1000, (current - last) / 1000, this.staticBalls);

			for(var j = this.staticBalls.length - 1; j >= 0; --j) {
				if(this.staticBalls[j].counter == 0) {
					++this.score;
					this.staticBalls.splice(j, 1);
				}
			}

			if(this.currentBall.ny < this.currentBall.nr && normalizeRadian(this.currentBall.direction) > Math.PI) {
				this.currentBall.state.s = 0;
				this.state = this.GAMEOVER();
			} else if(this.currentBall.state.s < 0.001) {
				if(this.currentBall.ny >= 0) {
					this.currentBall.grow(this.staticBalls);
					this.staticBalls.push(this.currentBall);
				}
				this.currentBall = null;
				break;
			}
			last = current;
		}
	}

	this.cannon.update(this.lastUpdateTime / 1000, (time - this.lastUpdateTime) / 1000);

	this.lastUpdateTime = time;
}

DontYouFillItGame.prototype.pause = function() {
	this.state = this.PAUSED();
};

DontYouFillItGame.prototype.resume = function() {
	this.lastUpdateTime = performance.now ? performance.now() : Date.now();
	this.state = this.RUNNING();
};

DontYouFillItGame.prototype.reset = function() {
	this.currentBall = null;
	this.staticBalls = [];
	this.cannon.state.u = 0;
	this.score = 0;
	this.resume();
};

DontYouFillItGame.prototype.fire = function() {
	this.currentBall = new Ball(
	    this.DEFAULT_BALL_RADIUS,
	    0.5 + Math.cos(this.cannon.getAngle()) * this.CANNON_LENGTH,
	    this.CANNON_Y_POSITION + this.CANNON_BASE_HEIGHT + Math.sin(this.cannon.getAngle()) * this.CANNON_LENGTH,
	    this.cannon.getAngle());
};
