export interface IPasswordChange {  
    isToken: string;
  email: string;
  phoneNumber?: string;
    currentPassword?: string;
    newPassword: string;
    confirmPassword: string;
}