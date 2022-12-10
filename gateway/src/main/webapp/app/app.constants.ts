// These constants are injected via webpack environment variables.
// You can add more variables in webpack.common.js or in profile specific webpack.<dev|prod>.js files.
// If you change the values in the webpack config files, you need to re run webpack to update the application

export const VERSION = process.env.VERSION;
export const DEBUG_INFO_ENABLED: boolean = !!process.env.DEBUG_INFO_ENABLED;
export const SERVER_API_URL = process.env.SERVER_API_URL;
export const BUILD_TIMESTAMP = process.env.BUILD_TIMESTAMP;
export const BASE_URL = process.env.BASE_URL;
export const ORCID_BASE_URL = process.env.ORCID_BASE_URL;
export const EMAIL_REGEXP = /^([^@\s]|(".+"))+@([^@\s\."'\(\)\[\]\{\}\\/,:;]+\.)+([^@\s\."'\(\)\[\]\{\}\\/,:;]{2,})+$/;
export const URL_REGEXP = /(http(s)?:\/\/.)?(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)/;
