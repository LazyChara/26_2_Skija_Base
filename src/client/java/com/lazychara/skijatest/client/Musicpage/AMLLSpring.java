package com.lazychara.skijatest.client.Musicpage;

final class AMLLSpring {
    private float currentPosition;
    private float targetPosition;
    private float currentTime;
    private float startPosition;
    private float startVelocity;
    private float mass = 1f;
    private float damping = 10f;
    private float stiffness = 100f;
    private boolean soft;
    private boolean queuedPosition;
    private float queuedTime;
    private float queuedTarget;

    AMLLSpring(float currentPosition) {
        setPosition(currentPosition);
    }

    void setPosition(float position) {
        currentPosition = position;
        targetPosition = position;
        currentTime = 0f;
        startPosition = position;
        startVelocity = 0f;
        queuedPosition = false;
    }

    void updateParams(float mass, float stiffness, float damping, boolean soft) {
        if (Math.abs(this.mass - mass) < 0.0001f
                && Math.abs(this.stiffness - stiffness) < 0.0001f
                && Math.abs(this.damping - damping) < 0.0001f
                && this.soft == soft) return;
        float position = solve(currentTime);
        float velocity = getCurrentVelocity();
        this.mass = mass;
        this.stiffness = stiffness;
        this.damping = damping;
        this.soft = soft;
        resetSolver(position, velocity);
    }

    void updateParams(float mass, float stiffness, float damping) {
        updateParams(mass, stiffness, damping, false);
    }

    void setTargetPosition(float targetPosition, float delay) {
        if (delay > 0f) {
            if (queuedPosition && Math.abs(queuedTarget - targetPosition) < 0.0001f) return;
            queuedPosition = true;
            queuedTime = delay;
            queuedTarget = targetPosition;
            return;
        }
        queuedPosition = false;
        if (Math.abs(this.targetPosition - targetPosition) < 0.0001f) return;
        float position = solve(currentTime);
        float velocity = getCurrentVelocity();
        this.targetPosition = targetPosition;
        resetSolver(position, velocity);
    }

    void update(float dt) {
        if (dt <= 0f) return;
        currentTime += dt;
        currentPosition = solve(currentTime);
        if (queuedPosition) {
            queuedTime -= dt;
            if (queuedTime <= 0f) {
                float target = queuedTarget;
                queuedPosition = false;
                setTargetPosition(target, 0f);
            }
        }
        if (arrived()) setPosition(targetPosition);
    }

    float getCurrentPosition() {
        return currentPosition;
    }

    float getCurrentVelocity() {
        return velocity(currentTime);
    }

    private void resetSolver(float position, float velocity) {
        currentPosition = position;
        startPosition = position;
        startVelocity = velocity;
        currentTime = 0f;
    }

    private boolean arrived() {
        return !queuedPosition
                && Math.abs(targetPosition - currentPosition) < 0.01f
                && Math.abs(getCurrentVelocity()) < 0.01f;
    }

    private float solve(float t) {
        float delta = targetPosition - startPosition;
        if (delta == 0f && startVelocity == 0f) return targetPosition;
        float safeMass = Math.max(0.0001f, mass);
        float safeStiffness = Math.max(0.0001f, stiffness);
        float dampingRatio = damping / (2f * (float) Math.sqrt(safeStiffness * safeMass));
        if (soft || dampingRatio >= 1f) {
            float angularFrequency = -(float) Math.sqrt(safeStiffness / safeMass);
            float leftover = -angularFrequency * delta - startVelocity;
            return targetPosition - (delta + t * leftover) * (float) Math.exp(t * angularFrequency);
        }
        float dampingFrequency = (float) Math.sqrt(Math.max(0.0001f, 4f * safeMass * safeStiffness - damping * damping));
        float leftover = (damping * delta - 2f * safeMass * startVelocity) / dampingFrequency;
        float dfm = (0.5f * dampingFrequency) / safeMass;
        float dm = -(0.5f * damping) / safeMass;
        return targetPosition - ((float) Math.cos(t * dfm) * delta + (float) Math.sin(t * dfm) * leftover) * (float) Math.exp(t * dm);
    }

    private float velocity(float t) {
        float delta = targetPosition - startPosition;
        if (delta == 0f && startVelocity == 0f) return 0f;
        float safeMass = Math.max(0.0001f, mass);
        float safeStiffness = Math.max(0.0001f, stiffness);
        float dampingRatio = damping / (2f * (float) Math.sqrt(safeStiffness * safeMass));
        if (soft || dampingRatio >= 1f) {
            float angularFrequency = -(float) Math.sqrt(safeStiffness / safeMass);
            float leftover = -angularFrequency * delta - startVelocity;
            float exp = (float) Math.exp(t * angularFrequency);
            return -exp * (leftover + angularFrequency * (delta + t * leftover));
        }
        float dampingFrequency = (float) Math.sqrt(Math.max(0.0001f, 4f * safeMass * safeStiffness - damping * damping));
        float leftover = (damping * delta - 2f * safeMass * startVelocity) / dampingFrequency;
        float dfm = (0.5f * dampingFrequency) / safeMass;
        float dm = -(0.5f * damping) / safeMass;
        float cos = (float) Math.cos(t * dfm);
        float sin = (float) Math.sin(t * dfm);
        float a = cos * delta + sin * leftover;
        float da = -dfm * sin * delta + dfm * cos * leftover;
        float exp = (float) Math.exp(t * dm);
        return -exp * (da + dm * a);
    }
}
