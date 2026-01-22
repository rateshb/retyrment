import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { Modal } from './Modal';

describe('Modal', () => {
  it('does not render when closed', () => {
    const { container } = render(
      <Modal isOpen={false} onClose={() => {}} title="Title">
        Content
      </Modal>
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders title and content when open', () => {
    render(
      <Modal isOpen onClose={() => {}} title="Title">
        Content
      </Modal>
    );
    expect(screen.getByText('Title')).toBeInTheDocument();
    expect(screen.getByText('Content')).toBeInTheDocument();
  });

  it('calls onClose on backdrop click and escape', () => {
    const onClose = vi.fn();
    const { container } = render(
      <Modal isOpen onClose={onClose} title="Title">
        Content
      </Modal>
    );
    const backdrop = container.querySelector('div.backdrop-blur-sm') as HTMLElement;
    fireEvent.click(backdrop);
    expect(onClose).toHaveBeenCalledTimes(1);

    fireEvent.keyDown(document, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledTimes(2);
  });
});
