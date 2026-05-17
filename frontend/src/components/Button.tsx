import { Icon } from './Icon'

type ButtonVariant = 'default' | 'primary' | 'danger' | 'ghost'
type ButtonSize = 'md' | 'sm'

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
  icon?: string
  iconAfter?: string
}

export function Button({
  children,
  variant = 'default',
  size = 'md',
  icon,
  iconAfter,
  className,
  ...rest
}: ButtonProps) {
  const iconSize = size === 'sm' ? 14 : 16
  return (
    <button
      className={`tti-btn tti-btn--${variant} tti-btn--${size}${className ? ` ${className}` : ''}`}
      {...rest}
    >
      {icon && <Icon name={icon as Parameters<typeof Icon>[0]['name']} size={iconSize} />}
      <span>{children}</span>
      {iconAfter && <Icon name={iconAfter as Parameters<typeof Icon>[0]['name']} size={iconSize} />}
    </button>
  )
}
