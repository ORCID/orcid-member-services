export default {
  '/userservice': {
    target: 'http://localhost:9000',
    secure: false,
    changeOrigin: true,
    rewrite: (path) => path.replace(/^\/userservice/, ''),
  },
  '/oauth2': {
    target: 'http://localhost:9000',
    secure: false,
    changeOrigin: true,
  },
  '/.well-known': {
    target: 'http://localhost:9000',
    secure: false,
    changeOrigin: true,
  },
  '/memberservice': {
    target: 'http://localhost:9010',
    secure: false,
    changeOrigin: true,
    rewrite: (path) => path.replace(/^\/memberservice/, ''),
  },
  '/assertionservice': {
    target: 'http://localhost:9020',
    secure: false,
    changeOrigin: true,
    rewrite: (path) => path.replace(/^\/assertionservice/, ''),
  },
}
