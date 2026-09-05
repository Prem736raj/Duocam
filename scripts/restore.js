const { execSync } = require('child_process');
try {
    const list = execSync('find /app -name "*.kt" 2>/dev/null').toString().trim().split('\n');
    console.log("All .kt files on the system:", list);
} catch(e) {
    console.error("Error:", e.message);
}
