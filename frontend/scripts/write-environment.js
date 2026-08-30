const fs = require('fs');
const path = require('path');

const apiUrl = process.env.NG_APP_API_URL || 'https://YOUR-BACKEND.onrender.com';
const target = path.join(__dirname, '..', 'src', 'environments', 'environment.prod.ts');

const contents = `export const environment = {
  production: true,
  apiUrl: '${apiUrl.replace(/'/g, "\\'")}'
};
`;

fs.writeFileSync(target, contents, 'utf8');
console.log(`Wrote environment.prod.ts with apiUrl=${apiUrl}`);
